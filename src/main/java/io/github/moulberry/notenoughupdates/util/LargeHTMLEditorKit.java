package io.github.moulberry.notenoughupdates.util;

import javax.swing.text.*;
import javax.swing.text.html.*;
import java.awt.*;
import java.awt.geom.AffineTransform;

public class LargeHTMLEditorKit extends HTMLEditorKit {

    ViewFactory factory = new MyViewFactory();

    @Override
    public ViewFactory getViewFactory() {
        return factory;
    }

    public Document createDefaultDocument() {
        HTMLDocument doc = (HTMLDocument)super.createDefaultDocument();
        doc.setAsynchronousLoadPriority(-1);
        return doc;
    }

    class MyViewFactory extends HTMLFactory {
        @Override
        public View create(Element elem) {
            AttributeSet attrs = elem.getAttributes();
            Object elementName = attrs.getAttribute(AbstractDocument.ElementNameAttribute);
            Object o = (elementName != null) ? null : attrs.getAttribute(StyleConstants.NameAttribute);
            if (o instanceof HTML.Tag) {
                HTML.Tag kind = (HTML.Tag) o;
                if (kind == HTML.Tag.HTML) {
                    return new HTMLBlockView(elem);
                }
            }
            View view = super.create(elem);
            if(view instanceof ImageView) {
                //((ImageView)view).setLoadsSynchronously(true);
            }
            return view;
        }

    }


    private class HTMLBlockView extends BlockView {

        public HTMLBlockView(Element elem) {
            super(elem,  View.Y_AXIS);
        }

        @Override
        protected void layout(int width, int height) {
            if (width<Integer.MAX_VALUE) {
                super.layout(new Double(width / getZoomFactor()).intValue(),
                        new Double(height *
                                getZoomFactor()).intValue());
            }
        }

        public double getZoomFactor() {
            Double scale = (Double) getDocument().getProperty("ZOOM_FACTOR");
            if (scale != null) {
                return scale.doubleValue();
            }

            return 1;
        }

        @Override
        public void paint(Graphics g, Shape allocation) {
            Graphics2D g2d = (Graphics2D) g;
            double zoomFactor = getZoomFactor();
            AffineTransform old = g2d.getTransform();
            g2d.scale(zoomFactor, zoomFactor);
            super.paint(g2d, allocation);
            g2d.setTransform(old);
        }

        @Override
        public float getMinimumSpan(int axis) {
            float f = super.getMinimumSpan(axis);
            f *= getZoomFactor();
            return f;
        }

        @Override
        public float getMaximumSpan(int axis) {
            float f = super.getMaximumSpan(axis);
            f *= getZoomFactor();
            return f;
        }

        @Override
        public float getPreferredSpan(int axis) {
            float f = super.getPreferredSpan(axis);
            f *= getZoomFactor();
            return f;
        }

        @Override
        public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
            double zoomFactor = getZoomFactor();
            Rectangle alloc;
            alloc = a.getBounds();
            Shape s = super.modelToView(pos, alloc, b);
            alloc = s.getBounds();
            alloc.x *= zoomFactor;
            alloc.y *= zoomFactor;
            alloc.width *= zoomFactor;
            alloc.height *= zoomFactor;

            return alloc;
        }

        @Override
        public int viewToModel(float x, float y, Shape a,
                               Position.Bias[] bias) {
            double zoomFactor = getZoomFactor();
            Rectangle alloc = a.getBounds();
            x /= zoomFactor;
            y /= zoomFactor;
            alloc.x /= zoomFactor;
            alloc.y /= zoomFactor;
            alloc.width /= zoomFactor;
            alloc.height /= zoomFactor;

            return super.viewToModel(x, y, alloc, bias);
        }

    }

}