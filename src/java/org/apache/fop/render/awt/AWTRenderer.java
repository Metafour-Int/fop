/*
 * Copyright 1999-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id$ */

package org.apache.fop.render.awt;

/*
 * originally contributed by
 * Juergen Verwohlt: Juergen.Verwohlt@jCatalog.com,
 * Rainer Steinkuhle: Rainer.Steinkuhle@jCatalog.com,
 * Stanislav Gorkhover: Stanislav.Gorkhover@jCatalog.com
 */

// Java
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.io.IOException;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.MimeConstants;
import org.apache.fop.area.Area;
import org.apache.fop.area.PageViewport;
import org.apache.fop.render.awt.viewer.PreviewDialog;
import org.apache.fop.render.awt.viewer.Renderable;
import org.apache.fop.render.awt.viewer.Translator;
import org.apache.fop.render.java2d.Java2DRenderer;

/**
 * The AWTRender outputs the pages generated by the layout engine to a Swing
 * window. This Swing window serves as default viewer for the -awt switch and as
 * an example of how to embed the AWTRenderer into an AWT/Swing application.
 */
public class AWTRenderer extends Java2DRenderer implements Pageable {

    /** The MIME type for AWT-Rendering */
    public static final String MIME_TYPE = MimeConstants.MIME_FOP_AWT_PREVIEW;

    /** The resource bundle used for AWT messages. */
    protected Translator translator = null;

    /** flag for debugging */
    public boolean debug;

    /** If true, preview dialog is shown. */
    public boolean dialogDisplay = true;

    /**
     * The preview dialog frame used for display of the documents. Also used as
     * the AWT Component for FontSetup in generating valid font measures.
     */
    protected PreviewDialog frame;

    /**
     * Renderable instance that can be used to reload and re-render a document after 
     * modifications.
     */
    protected Renderable renderable;
    
    /**
     * Creates a new AWTRenderer instance.
     */
    public AWTRenderer() {
        translator = new Translator();
    }

    /** @see org.apache.fop.render.Renderer#setUserAgent(org.apache.fop.apps.FOUserAgent) */
    public void setUserAgent(FOUserAgent foUserAgent) {
        super.setUserAgent(foUserAgent);
        if (dialogDisplay) {
            createPreviewDialog();
        }
    }

    /**
     * A Renderable instance can be set so the Preview Dialog can enable the "Reload" button
     * which causes the current document to be reprocessed and redisplayed.
     * @param renderable the Renderable instance.
     */
    public void setRenderable(Renderable renderable) {
        this.renderable = renderable;
    }
    
    /**
     * Sets whether the preview dialog should be created and displayed when
     * the rendering is finished.
     * @param show If false, preview dialog is not shown. True by default
     */
    public void setPreviewDialogDisplayed(boolean show) {
        dialogDisplay = show;
    }

    /** 
     * @see org.apache.fop.render.Renderer#renderPage(org.apache.fop.area.PageViewport)
     */
    public void renderPage(PageViewport pageViewport) throws IOException {

        super.renderPage(pageViewport);
        if (frame != null) {
            frame.setInfo();
        }
    }

    /** @see org.apache.fop.render.Renderer#stopRenderer() */
    public void stopRenderer() throws IOException {
        super.stopRenderer();
        if (frame != null) {
            frame.setStatus(translator.getString("Status.Show"));
            frame.reload(); // Refreshes view of page
        }
    }

    /**
     * @return the dimensions of the specified page
     * @param pageNum the page number
     * @exception FOPException If the page is out of range or has not been rendered.
     */
    public Dimension getPageImageSize(int pageNum) throws FOPException {
        Rectangle2D bounds = getPageViewport(pageNum).getViewArea();
        pageWidth = (int) Math.round(bounds.getWidth() / 1000f);
        pageHeight = (int) Math.round(bounds.getHeight() / 1000f);
        double scaleX = scaleFactor 
                * (25.4 / FOUserAgent.DEFAULT_TARGET_RESOLUTION)
                / userAgent.getTargetPixelUnitToMillimeter();
        double scaleY = scaleFactor 
                * (25.4 / FOUserAgent.DEFAULT_TARGET_RESOLUTION)
                / userAgent.getTargetPixelUnitToMillimeter();
        int bitmapWidth = (int) ((pageWidth * scaleX) + 0.5);
        int bitmapHeight = (int) ((pageHeight * scaleY) + 0.5);
                return new Dimension(bitmapWidth, bitmapHeight);
    }

    /** Creates and initialize the AWT Viewer main window */
    private PreviewDialog createPreviewDialog() {
        frame = new PreviewDialog(userAgent, this.renderable);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent we) {
                System.exit(0);
            }
        });

        // Centers the window
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = frame.getSize();
        if (frameSize.height > screenSize.height) {
            frameSize.height = screenSize.height;
        }
        if (frameSize.width > screenSize.width) {
            frameSize.width = screenSize.width;
        }
        frame.setLocation((screenSize.width - frameSize.width) / 2,
                (screenSize.height - frameSize.height) / 2);
        frame.setStatus(translator.getString("Status.Build.FO.tree"));
        frame.setVisible(true);
        return frame;
    }

    /** @see java.awt.print.Pageable#getPageFormat(int) */
    public PageFormat getPageFormat(int pageIndex)
            throws IndexOutOfBoundsException {
        try {
            if (pageIndex >= getNumberOfPages()) {
                return null;
            }
    
            PageFormat pageFormat = new PageFormat();
    
            Paper paper = new Paper();
            pageFormat.setPaper(paper);
    
            Rectangle2D dim = getPageViewport(pageIndex).getViewArea();
            double width = dim.getWidth();
            double height = dim.getHeight();
    
            // if the width is greater than the height assume lanscape mode
            // and swap the width and height values in the paper format
            if (width > height) {
                paper.setImageableArea(0, 0, height / 1000d, width / 1000d);
                paper.setSize(height / 1000d, width / 1000d);
                pageFormat.setOrientation(PageFormat.LANDSCAPE);
            } else {
                paper.setImageableArea(0, 0, width / 1000d, height / 1000d);
                paper.setSize(width / 1000d, height / 1000d);
                pageFormat.setOrientation(PageFormat.PORTRAIT);
            }
            return pageFormat;
        } catch (FOPException fopEx) {
            throw new IndexOutOfBoundsException(fopEx.getMessage());
        }
    }

    /** @see java.awt.print.Pageable#getPrintable(int) */
    public Printable getPrintable(int pageIndex)
            throws IndexOutOfBoundsException {
        return this;
    }

    /** @see org.apache.fop.render.Renderer */
    public boolean supportsOutOfOrder() {
        return true; // TODO true?
    }

    /** @return the Translator for this renderer */
    public Translator getTranslator() {
        return translator;
    }

    /** @see org.apache.fop.render.AbstractRenderer */
    public String getMimeType() {
        return MIME_TYPE;
    }

    /**
     * Draws the background and borders and adds a basic debug view // TODO
     * implement visual-debugging as standalone
     *
     * @see org.apache.fop.render.java2d.Java2DRenderer#drawBackAndBorders(org.apache.fop.area.Area,
     * float, float, float, float)
     *
     * @param area the area to get the traits from
     * @param startx the start x position
     * @param starty the start y position
     * @param width the width of the area
     * @param height the height of the area
     */
    protected void drawBackAndBorders(Area area, float startx, float starty,
            float width, float height) {

        if (debug) {
            debugBackAndBorders(area, startx, starty, width, height);
        }

        super.drawBackAndBorders(area, startx, starty, width, height);
    }

    /** Draws a thin border around every area to help debugging */
    private void debugBackAndBorders(Area area, float startx, float starty,
            float width, float height) {

        // saves the graphics state in a stack
        saveGraphicsState();

        Color col = new Color(0.7f, 0.7f, 0.7f);
        state.updateColor(col);
        state.updateStroke(0.4f, EN_SOLID);
        state.getGraph().draw(
                new Rectangle2D.Float(startx, starty, width, height));

        // restores the last graphics state from the stack
        restoreGraphicsState();
    }

}
