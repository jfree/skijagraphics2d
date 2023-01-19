/* ===============
 * SkijaGraphics2D
 * ===============
 *
 * (C)opyright 2021-present, by David Gilbert.
 *
 * The SkijaGraphics2D class has been developed by David Gilbert for
 * use with Orson Charts (https://github.com/jfree/orson-charts) and
 * JFreeChart (https://www.jfree.org/jfreechart).  It may be useful for other
 * code that uses the Graphics2D API provided by Java2D.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *   - Neither the name of the Object Refinery Limited nor the
 *     names of its contributors may be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL OBJECT REFINERY LIMITED BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package org.jfree.skija;

import io.github.humbleui.skija.BlendMode;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.ColorAlphaType;
import io.github.humbleui.skija.ColorType;
import io.github.humbleui.skija.FilterTileMode;
import io.github.humbleui.skija.FontStyle;
import io.github.humbleui.skija.GradientStyle;
import io.github.humbleui.skija.ImageInfo;
import io.github.humbleui.skija.Matrix33;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.skija.PaintStrokeCap;
import io.github.humbleui.skija.PaintStrokeJoin;
import io.github.humbleui.skija.Path;
import io.github.humbleui.skija.PathEffect;
import io.github.humbleui.skija.PathFillMode;
import io.github.humbleui.skija.Shader;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.skija.Typeface;
import io.github.humbleui.types.Rect;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import static java.awt.MultipleGradientPaint.CycleMethod.NO_CYCLE;
import static java.awt.MultipleGradientPaint.CycleMethod.REFLECT;
import static java.awt.MultipleGradientPaint.CycleMethod.REPEAT;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferInt;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of the Graphics2D API that targets the Skija graphics API
 * (https://github.com/JetBrains/skija).
 */
public final class SkijaGraphics2D extends Graphics2D {

    /** SkijaGraphics2D version */
    public static final String VERSION = Version.getVersion();

    private static final Logger LOGGER = LoggerFactory.getLogger(SkijaGraphics2D.class);

    /** The line width to use when a BasicStroke with line width = 0.0 is applied. */
    private static final double MIN_LINE_WIDTH = 0.1;

    /** default paint */
    private static final Color DEFAULT_PAINT = Color.BLACK;
    /** default stroke */
    private static final Stroke DEFAULT_STROKE = new BasicStroke(1.0f);
    /** default font */
    private static final Font DEFAULT_FONT = new Font("SansSerif", Font.PLAIN, 12);

    /** font mapping */
    private final static Map<String, String> FONT_MAPPING = createDefaultFontMap();

    private final static Map<TypefaceKey, Typeface> TYPEFACE_MAP = Collections.synchronizedMap(new HashMap<>(16));

    /** 
     * The font render context.  The fractional metrics flag solves the glyph
     * positioning issue identified by Christoph Nahr:
     * http://news.kynosarges.org/2014/06/28/glyph-positioning-in-jfreesvg-orsonpdf/
     */
    private final static FontRenderContext DEFAULT_FONT_RENDER_CONTEXT = new FontRenderContext(
            null, false, true);

    /* members */
    /** log enabled in constructor if logger is at debug level (low perf overhead) */
    private final boolean LOG_ENABLED;

    /** Rendering hints. */
    private final RenderingHints hints = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_DEFAULT);

    /** Surface from Skija */
    private Surface surface;

    private int width;
    private int height;

    /** Canvas from Skija */
    private Canvas canvas;

    /** Paint used for drawing on Skija canvas. */
    private io.github.humbleui.skija.Paint skijaPaint;

    /** The Skija save/restore count, used to restore the original clip in setClip(). */
    private int restoreCount;

    private Paint awtPaint;

    /** Stores the AWT Color object for get/setColor(). */
    private Color color;

    private Stroke stroke;

    private Font awtFont;

    private Typeface typeface;

    private io.github.humbleui.skija.Font skijaFont;

    /** The background color, used in the {@code clearRect()} method. */
    private Color background;

    private AffineTransform transform = new AffineTransform();

    private Composite composite = AlphaComposite.getInstance(
            AlphaComposite.SRC_OVER, 1.0f);

    /** The user clip (can be null). */
    Shape clip;

    /** 
     * The font render context.
     */
    private final FontRenderContext fontRenderContext = DEFAULT_FONT_RENDER_CONTEXT;

    /**
     * An instance that is lazily instantiated in drawLine and then 
     * subsequently reused to avoid creating a lot of garbage.
     */
    private Line2D line;

    /**
     * An instance that is lazily instantiated in fillRect and then 
     * subsequently reused to avoid creating a lot of garbage.
     */
    private Rectangle2D rect;

    /**
     * An instance that is lazily instantiated in draw/fillRoundRect and then
     * subsequently reused to avoid creating a lot of garbage.
     */
    private RoundRectangle2D roundRect;

    /**
     * An instance that is lazily instantiated in draw/fillOval and then
     * subsequently reused to avoid creating a lot of garbage.
     */
    private Ellipse2D oval;

    /**
     * An instance that is lazily instantiated in draw/fillArc and then
     * subsequently reused to avoid creating a lot of garbage.
     */
    private Arc2D arc;

    /**
     * The device configuration (this is lazily instantiated in the
     * getDeviceConfiguration() method).
     */
    private GraphicsConfiguration deviceConfiguration;

    /** Used and reused in the path() method below. */
    private final double[] coords = new double[6];

    /** Used and reused in the drawString() method below. */
    private final StringBuilder sbStr = new StringBuilder(256);

    /**
     * Throws an {@code IllegalArgumentException} if {@code arg} is
     * {@code null}.
     * 
     * @param arg  the argument to check.
     * @param name  the name of the argument (to display in the exception 
     *         message).
     */
    private static void nullNotPermitted(Object arg, String name) {
        if (arg == null) {
            throw new IllegalArgumentException("Null '" + name + "' argument.");
        }
    }

    /**
     * Creates a map containing default mappings from the Java logical font names
     * to suitable physical font names.  This is not a particularly great solution,
     * but so far I don't see a better alternative.
     *
     * @return A map.
     */
    public static Map<String, String> createDefaultFontMap() {
        final Map<String, String> result = new HashMap<>(8);
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) { // Windows
            result.put(Font.MONOSPACED, "Courier New");
            result.put(Font.SANS_SERIF, "Arial");
            result.put(Font.SERIF, "Times New Roman");
        } else if (os.contains("mac")) { // MacOS
            result.put(Font.MONOSPACED, "Courier New");
            result.put(Font.SANS_SERIF, "Helvetica");
            result.put(Font.SERIF, "Times New Roman");
        } else { // assume Linux
            result.put(Font.MONOSPACED, "Courier New");
            result.put(Font.SANS_SERIF, "Arial");
            result.put(Font.SERIF, "Times New Roman");
        }
        result.put(Font.DIALOG, result.get(Font.SANS_SERIF));
        result.put(Font.DIALOG_INPUT, result.get(Font.SANS_SERIF));
        return result;
    }

    /**
     * Creates a new instance with the specified height and width.
     *
     * @param width  the width.
     * @param height  the height.
     */
    public SkijaGraphics2D(final int width, final int height) {
        LOG_ENABLED = LOGGER.isDebugEnabled();
        if (LOG_ENABLED) {
            LOGGER.debug("SkijaGraphics2D({}, {})", width, height);
        }
        this.width = width;
        this.height = height;
        this.surface = Surface.makeRasterN32Premul(width, height);
        init(surface.getCanvas());
    }

    /**
     * Creates a new instance with the specified height and width using an existing
     * canvas.
     *
     * @param canvas  the canvas ({@code null} not permitted).
     */
    public SkijaGraphics2D(final Canvas canvas) {
        LOG_ENABLED = LOGGER.isDebugEnabled();
        if (LOG_ENABLED) {
            LOGGER.debug("SkijaGraphics2D(Canvas)");
        }
        init(canvas);
    }

    /**
     * Copy-constructor: creates a new instance with the given parent SkijaGraphics2D.
     *
     * @param parent SkijaGraphics2D instance to copy ({@code null} not permitted).
     */
    private SkijaGraphics2D(final SkijaGraphics2D parent) {
        LOG_ENABLED = LOGGER.isDebugEnabled();
        if (LOG_ENABLED) {
            LOGGER.debug("SkijaGraphics2D(parent)");
        }
        nullNotPermitted(parent, "parent");

        this.canvas = parent.getCanvas();

        nullNotPermitted(canvas, "canvas");

        this.setRenderingHints(parent.getRenderingHintsInternally());

        if (getRenderingHint(SkijaHints.KEY_FONT_MAPPING_FUNCTION) == null) {
            setRenderingHint(SkijaHints.KEY_FONT_MAPPING_FUNCTION, (Function<String, String>) s -> FONT_MAPPING.get(s));
        }
        this.clip = parent.clip;
        this.setBackground(parent.getBackground());
        this.skijaPaint = new io.github.humbleui.skija.Paint().setColor(DEFAULT_PAINT.getRGB());
        this.setPaint(parent.getPaint());
        this.setComposite(parent.getComposite());
        this.setStroke(parent.getStroke());
        this.setFont(parent.getFont());
        this.setTransform(parent.getTransformInternally());

        // save the original clip settings so they can be restored later in setClip()
        this.restoreCount = this.canvas.save();
        if (LOG_ENABLED) {
            LOGGER.debug("restoreCount updated to {}", this.restoreCount);
        }
    }

    /**
     * Creates a new instance using an existing canvas.
     *
     * @param canvas  the canvas ({@code null} not permitted).
     */
    private void init(final Canvas canvas) {
        nullNotPermitted(canvas, "canvas");
        this.canvas = canvas;

        if (getRenderingHint(SkijaHints.KEY_FONT_MAPPING_FUNCTION) == null) {
            setRenderingHint(SkijaHints.KEY_FONT_MAPPING_FUNCTION, (Function<String, String>) s -> FONT_MAPPING.get(s));
        }

        // use constants for quick initialization:
        this.background = DEFAULT_PAINT;
        this.skijaPaint = new io.github.humbleui.skija.Paint().setColor(DEFAULT_PAINT.getRGB());
        setPaint(DEFAULT_PAINT);
        setStroke(DEFAULT_STROKE);
        // use TYPEFACE_MAP cache:
        setFont(DEFAULT_FONT);

        // save the original clip settings so they can be restored later in setClip()
        this.restoreCount = this.canvas.save();
        if (LOG_ENABLED) {
            LOGGER.debug("restoreCount updated to {}", this.restoreCount);
        }
    }

    private Canvas getCanvas() {
        return this.canvas;
    }

    /**
     * Returns the Skija surface that was created by this instance, or {@code null}.
     *
     * @return The Skija surface (possibly {@code null}).
     */
    public Surface getSurface() {
        return this.surface;
    }

    /**
     * Creates a Skija path from the outline of a Java2D shape.
     *
     * @param shape  the shape ({@code null} not permitted).
     *
     * @return A path.
     */
    private Path path(final Shape shape) {
        final Path p = new Path(); // TODO: reuse Path instances or not (async safety) ?

        for (final PathIterator iterator = shape.getPathIterator(null); !iterator.isDone(); iterator.next()) {
            final int segType = iterator.currentSegment(coords);
            switch (segType) {
                case PathIterator.SEG_MOVETO:
                    if (LOG_ENABLED) {
                        LOGGER.debug("SEG_MOVETO: ({},{})", coords[0], coords[1]);
                    }
                    p.moveTo((float) coords[0], (float) coords[1]);
                    break;
                case PathIterator.SEG_LINETO:
                    if (LOG_ENABLED) {
                        LOGGER.debug("SEG_LINETO: ({},{})", coords[0], coords[1]);
                    }
                    p.lineTo((float) coords[0], (float) coords[1]);
                    break;
                case PathIterator.SEG_QUADTO:
                    if (LOG_ENABLED) {
                        LOGGER.debug("SEG_QUADTO: ({},{} {},{})", coords[0], coords[1], coords[2], coords[3]);
                    }
                    p.quadTo((float) coords[0], (float) coords[1],
                            (float) coords[2], (float) coords[3]);
                    break;
                case PathIterator.SEG_CUBICTO:
                    if (LOG_ENABLED) {
                        LOGGER.debug("SEG_CUBICTO: ({},{} {},{} {},{})", coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
                    }
                    p.cubicTo((float) coords[0], (float) coords[1],
                            (float) coords[2], (float) coords[3],
                            (float) coords[4], (float) coords[5]);
                    break;
                case PathIterator.SEG_CLOSE:
                    if (LOG_ENABLED) {
                        LOGGER.debug("SEG_CLOSE: ");
                    }
                    p.closePath();
                    break;
                default:
                    throw new RuntimeException("Unrecognised segment type " + segType);
            }
        }
        return p;
    }

    /**
     * Draws the specified shape with the current {@code paint} and
     * {@code stroke}.  There is direct handling for {@code Line2D} and
     * {@code Rectangle2D}.  All other shapes are mapped to a {@code GeneralPath}
     * and then drawn (effectively as {@code Path2D} objects).
     *
     * @param s  the shape ({@code null} not permitted).
     *
     * @see #fill(java.awt.Shape)
     */
    @Override
    public void draw(Shape s) {
        if (LOG_ENABLED) {
            LOGGER.debug("draw(Shape) : {}", s);
        }
        this.skijaPaint.setMode(PaintMode.STROKE);
        if (s instanceof Line2D) {
            Line2D l = (Line2D) s;
            this.canvas.drawLine((float) l.getX1(), (float) l.getY1(), (float) l.getX2(), (float) l.getY2(), this.skijaPaint);
        } else if (s instanceof Rectangle2D) {
            Rectangle2D r = (Rectangle2D) s;
            if (r.getWidth() <= 0.0 || r.getHeight() <= 0.0) {
                return;
            }
            this.canvas.drawRect(Rect.makeXYWH((float) r.getX(), (float) r.getY(), (float) r.getWidth(), (float) r.getHeight()), this.skijaPaint);
        } else if (s instanceof Ellipse2D) {
            Ellipse2D e = (Ellipse2D) s;
            this.canvas.drawOval(Rect.makeXYWH((float) e.getMinX(), (float) e.getMinY(), (float) e.getWidth(), (float) e.getHeight()), this.skijaPaint);
        } else {
            try (final Path p = path(s)) {
                this.canvas.drawPath(p, this.skijaPaint);
            } // `p` will be freed right here
        }
    }

    /**
     * Fills the specified shape with the current {@code paint}.  There is
     * direct handling for {@code Rectangle2D}.
     * All other shapes are mapped to a path outline and then filled.
     *
     * @param s  the shape ({@code null} not permitted).
     *
     * @see #draw(java.awt.Shape)
     */
    @Override
    public void fill(Shape s) {
        if (LOG_ENABLED) {
            LOGGER.debug("fill({})", s);
        }
        this.skijaPaint.setMode(PaintMode.FILL);
        if (s instanceof Rectangle2D) {
            Rectangle2D r = (Rectangle2D) s;
            if (r.getWidth() <= 0.0 || r.getHeight() <= 0.0) {
                return;
            }
            this.canvas.drawRect(Rect.makeXYWH((float) r.getX(), (float) r.getY(), (float) r.getWidth(), (float) r.getHeight()), this.skijaPaint);
        } else if (s instanceof Ellipse2D) {
            Ellipse2D e = (Ellipse2D) s;
            this.canvas.drawOval(Rect.makeXYWH((float) e.getMinX(), (float) e.getMinY(), (float) e.getWidth(), (float) e.getHeight()), this.skijaPaint);
        } else if (s instanceof Path2D) {
            final Path2D p2d = (Path2D) s;

            try (final Path p = path(s)) {
                if (p2d.getWindingRule() == Path2D.WIND_EVEN_ODD) {
                    p.setFillMode(PathFillMode.EVEN_ODD);
                } else {
                    p.setFillMode(PathFillMode.WINDING);
                }
                this.canvas.drawPath(p, this.skijaPaint);
            } // `p` will be freed right here
        } else {
            try (final Path p = path(s)) {
                this.canvas.drawPath(p, this.skijaPaint);
            } // `p` will be freed right here
        }
    }

    /**
     * Draws an image with the specified transform. Note that the 
     * {@code observer} is ignored in this implementation.     
     * 
     * @param img  the image.
     * @param xform  the transform ({@code null} permitted).
     * @param obs  the image observer (ignored).
     * 
     * @return {@code true} if the image is drawn. 
     */
    @Override
    public boolean drawImage(Image img, AffineTransform xform,
                             ImageObserver obs) {
        if (LOG_ENABLED) {
            LOGGER.debug("drawImage(Image, AffineTransform, ImageObserver)");
        }
        AffineTransform savedTransform = getTransform();
        if (xform != null) {
            transform(xform);
        }
        boolean result = drawImage(img, 0, 0, obs);
        if (xform != null) {
            setTransform(savedTransform);
        }
        return result;
    }

    /**
     * Draws the image resulting from applying the {@code BufferedImageOp}
     * to the specified image at the location {@code (x, y)}.
     *
     * @param img  the image.
     * @param op  the operation ({@code null} permitted).
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     */
    @Override
    public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) {
        if (LOG_ENABLED) {
            LOGGER.debug("drawImage(BufferedImage, BufferedImageOp, {}, {})", x, y);
        }
        BufferedImage imageToDraw = img;
        if (op != null) {
            imageToDraw = op.filter(img, null);
        }
        drawImage(imageToDraw, new AffineTransform(1f, 0f, 0f, 1f, x, y), null);
    }

    /**
     * Draws the rendered image. When {@code img} is {@code null} this method
     * does nothing.
     *
     * @param img  the image ({@code null} permitted).
     * @param xform  the transform.
     */
    @Override
    public void drawRenderedImage(RenderedImage img, AffineTransform xform) {
        if (LOG_ENABLED) {
            LOGGER.debug("drawRenderedImage(RenderedImage, AffineTransform)");
        }
        if (img == null) { // to match the behaviour specified in the JDK
            return;
        }
        BufferedImage bi = convertRenderedImage(img);
        drawImage(bi, xform, null);
    }

    /**
     * Draws the renderable image.
     * 
     * @param img  the renderable image.
     * @param xform  the transform.
     */
    @Override
    public void drawRenderableImage(RenderableImage img,
                                    AffineTransform xform) {
        if (LOG_ENABLED) {
            LOGGER.debug("drawRenderableImage(RenderableImage, AffineTransform xform)");
        }
        RenderedImage ri = img.createDefaultRendering();
        drawRenderedImage(ri, xform);
    }

    /**
     * Draws a string at {@code (x, y)}.  The start of the text at the
     * baseline level will be aligned with the {@code (x, y)} point.
     * 
     * @param str  the string ({@code null} not permitted).
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * 
     * @see #drawString(java.lang.String, float, float) 
     */
    @Override
    public void drawString(String str, int x, int y) {
        if (LOG_ENABLED) {
            LOGGER.debug("drawString({}, {}, {}", str, x, y);
        }
        drawString(str, (float) x, (float) y);
    }

    /**
     * Draws a string at {@code (x, y)}. The start of the text at the
     * baseline level will be aligned with the {@code (x, y)} point.
     * 
     * @param str  the string ({@code null} not permitted).
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     */
    @Override
    public void drawString(String str, float x, float y) {
        if (str == null) {
            throw new NullPointerException("Null 'str' argument.");
        }
        if (LOG_ENABLED) {
            LOGGER.debug("drawString({}, {}, {})", str, x, y);
        }
        this.skijaPaint.setMode(PaintMode.FILL);
        this.canvas.drawString(str, x, y, this.skijaFont, this.skijaPaint);
    }

    /**
     * Draws a string of attributed characters at {@code (x, y)}.  The 
     * call is delegated to 
     * {@link #drawString(AttributedCharacterIterator, float, float)}. 
     * 
     * @param iterator  an iterator for the characters.
     * @param x  the x-coordinate.
     * @param y  the x-coordinate.
     */
    @Override
    public void drawString(AttributedCharacterIterator iterator, int x, int y) {
        if (LOG_ENABLED) {
            LOGGER.debug("drawString(AttributedCharacterIterator, {}, {}", x, y);
        }
        drawString(iterator, (float) x, (float) y);
    }

    /**
     * Draws a string of attributed characters at {@code (x, y)}. 
     * 
     * @param iterator  an iterator over the characters ({@code null} not 
     *     permitted).
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     */
    @Override
    public void drawString(AttributedCharacterIterator iterator, float x,
                           float y) {
        if (LOG_ENABLED) {
            LOGGER.debug("drawString(AttributedCharacterIterator, {}, {}", x, y);
        }
        final Set<AttributedCharacterIterator.Attribute> s = iterator.getAllAttributeKeys();
        if (!s.isEmpty()) {
            final TextLayout layout = new TextLayout(iterator, getFontRenderContext());
            layout.draw(this, x, y);
        } else {
            final StringBuilder sb = sbStr; // not thread-safe
            sb.setLength(0);
            iterator.first();
            for (int i = iterator.getBeginIndex(); i < iterator.getEndIndex(); i++, iterator.next()) {
                sb.append(iterator.current());
            }
            drawString(sb.toString(), x, y);
        }
    }

    /**
     * Draws the specified glyph vector at the location {@code (x, y)}.
     * 
     * @param g  the glyph vector ({@code null} not permitted).
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     */
    @Override
    public void drawGlyphVector(GlyphVector g, float x, float y) {
        if (LOG_ENABLED) {
            LOGGER.debug("drawGlyphVector(GlyphVector, {}, {})", x, y);
        }
        fill(g.getOutline(x, y));
    }

    /**
     * Returns {@code true} if the rectangle (in device space) intersects
     * with the shape (the interior, if {@code onStroke} is {@code false}, 
     * otherwise the stroked outline of the shape).
     * 
     * @param rect  a rectangle (in device space).
     * @param s the shape.
     * @param onStroke  test the stroked outline only?
     * 
     * @return A boolean. 
     */
    @Override
    public boolean hit(final Rectangle rect, final Shape s, final boolean onStroke) {
        if (LOG_ENABLED) {
            LOGGER.debug("hit(Rectangle, Shape, boolean)");
        }
        final Shape ts;
        if (onStroke) {
            ts = _createTransformedShape(this.stroke.createStrokedShape(s), false);
        } else {
            ts = _createTransformedShape(s, false);
        }
        if (!rect.getBounds2D().intersects(ts.getBounds2D())) {
            return false;
        }
        // note: Area class is very slow (especially for rect):
        final Area a1 = new Area(rect);
        final Area a2 = new Area(ts);
        a1.intersect(a2);
        return !a1.isEmpty();
    }

    /**
     * Returns the device configuration associated with this
     * {@code Graphics2D}.
     *
     * @return The device configuration (never {@code null}).
     */
    @Override
    public GraphicsConfiguration getDeviceConfiguration() {
        if (this.deviceConfiguration == null) {
            int w = this.width;
            int h = this.height;
            this.deviceConfiguration = new SkijaGraphicsConfiguration(w, h);
        }
        return this.deviceConfiguration;
    }

    /**
     * Sets the composite (only {@code AlphaComposite} is handled).
     * 
     * @param comp  the composite ({@code null} not permitted).
     * 
     * @see #getComposite() 
     */
    @Override
    public void setComposite(Composite comp) {
        if (LOG_ENABLED) {
            LOGGER.debug("setComposite({})", comp);
        }
        if (comp == null) {
            throw new IllegalArgumentException("Null 'comp' argument.");
        }
        this.composite = comp;
        if (comp instanceof AlphaComposite) {
            AlphaComposite ac = (AlphaComposite) comp;
            this.skijaPaint.setAlphaf(ac.getAlpha());

            switch (ac.getRule()) {
                case AlphaComposite.CLEAR:
                    this.skijaPaint.setBlendMode(BlendMode.CLEAR);
                    break;
                case AlphaComposite.SRC:
                    this.skijaPaint.setBlendMode(BlendMode.SRC);
                    break;
                case AlphaComposite.SRC_OVER:
                    this.skijaPaint.setBlendMode(BlendMode.SRC_OVER);
                    break;
                case AlphaComposite.DST_OVER:
                    this.skijaPaint.setBlendMode(BlendMode.DST_OVER);
                    break;
                case AlphaComposite.SRC_IN:
                    this.skijaPaint.setBlendMode(BlendMode.SRC_IN);
                    break;
                case AlphaComposite.DST_IN:
                    this.skijaPaint.setBlendMode(BlendMode.DST_IN);
                    break;
                case AlphaComposite.SRC_OUT:
                    this.skijaPaint.setBlendMode(BlendMode.SRC_OUT);
                    break;
                case AlphaComposite.DST_OUT:
                    this.skijaPaint.setBlendMode(BlendMode.DST_OUT);
                    break;
                case AlphaComposite.DST:
                    this.skijaPaint.setBlendMode(BlendMode.DST);
                    break;
                case AlphaComposite.SRC_ATOP:
                    this.skijaPaint.setBlendMode(BlendMode.SRC_ATOP);
                    break;
                case AlphaComposite.DST_ATOP:
                    this.skijaPaint.setBlendMode(BlendMode.DST_ATOP);
                    break;
            }
        }
    }

    @Override
    public void setPaint(Paint paint) {
        if (LOG_ENABLED) {
            LOGGER.debug("setPaint({})", paint);
        }
        if (paint == null) {
            return;
        }
        if (paintsAreEqual(paint, this.awtPaint)) {
            return;
        }
        this.awtPaint = paint;
        if (paint instanceof Color) {
            final Color c = (Color) paint;
            this.color = c;
            this.skijaPaint.setShader(Shader.makeColor(c.getRGB()));
        } else if (paint instanceof LinearGradientPaint) {
            final LinearGradientPaint lgp = (LinearGradientPaint) paint;
            float x0 = (float) lgp.getStartPoint().getX();
            float y0 = (float) lgp.getStartPoint().getY();
            float x1 = (float) lgp.getEndPoint().getX();
            float y1 = (float) lgp.getEndPoint().getY();

            final int[] colors = new int[lgp.getColors().length];
            for (int i = 0; i < lgp.getColors().length; i++) {
                colors[i] = lgp.getColors()[i].getRGB();
            }
            final float[] fractions = lgp.getFractions();
            final GradientStyle gs = GradientStyle.DEFAULT.withTileMode(awtCycleMethodToSkijaFilterTileMode(lgp.getCycleMethod()));
            this.skijaPaint.setShader(Shader.makeLinearGradient(x0, y0, x1, y1, colors, fractions, gs));
        } else if (paint instanceof RadialGradientPaint) {
            final RadialGradientPaint rgp = (RadialGradientPaint) paint;
            float x = (float) rgp.getCenterPoint().getX();
            float y = (float) rgp.getCenterPoint().getY();

            final int[] colors = new int[rgp.getColors().length];
            for (int i = 0; i < rgp.getColors().length; i++) {
                colors[i] = rgp.getColors()[i].getRGB();
            }
            final GradientStyle gs = GradientStyle.DEFAULT.withTileMode(awtCycleMethodToSkijaFilterTileMode(rgp.getCycleMethod()));
            float fx = (float) rgp.getFocusPoint().getX();
            float fy = (float) rgp.getFocusPoint().getY();

            final Shader shader;
            if (rgp.getFocusPoint().equals(rgp.getCenterPoint())) {
                shader = Shader.makeRadialGradient(x, y, rgp.getRadius(), colors, rgp.getFractions(), gs);
            } else {
                shader = Shader.makeTwoPointConicalGradient(fx, fy, 0, x, y, rgp.getRadius(), colors, rgp.getFractions(), gs);
            }
            this.skijaPaint.setShader(shader);
        } else if (paint instanceof GradientPaint) {
            final GradientPaint gp = (GradientPaint) paint;
            float x1 = (float) gp.getPoint1().getX();
            float y1 = (float) gp.getPoint1().getY();
            float x2 = (float) gp.getPoint2().getX();
            float y2 = (float) gp.getPoint2().getY();

            final int[] colors = new int[]{gp.getColor1().getRGB(), gp.getColor2().getRGB()};
            final GradientStyle gs = (gp.isCyclic())
                    ? GradientStyle.DEFAULT.withTileMode(FilterTileMode.MIRROR)
                    : GradientStyle.DEFAULT;

            this.skijaPaint.setShader(Shader.makeLinearGradient(x1, y1, x2, y2, colors, (float[]) null, gs));
        }
    }

    /**
     * Sets the stroke that will be used to draw shapes.  
     * 
     * @param s  the stroke ({@code null} not permitted).
     * 
     * @see #getStroke() 
     */
    @Override
    public void setStroke(Stroke s) {
        nullNotPermitted(s, "s");
        if (LOG_ENABLED) {
            LOGGER.debug("setStroke({})", stroke);
        }
        if (s == this.stroke) { // quick test, full equals test later
            return;
        }
        if (stroke instanceof BasicStroke) {
            BasicStroke bs = (BasicStroke) s;
            if (bs.equals(this.stroke)) {
                return; // no change
            }
            this.skijaPaint.setStrokeWidth((float) Math.max(bs.getLineWidth(), MIN_LINE_WIDTH));
            this.skijaPaint.setStrokeCap(awtToSkijaLineCap(bs.getEndCap()));
            this.skijaPaint.setStrokeJoin(awtToSkijaLineJoin(bs.getLineJoin()));
            this.skijaPaint.setStrokeMiter(bs.getMiterLimit());
            
            final float[] dashes = bs.getDashArray();
            if (dashes != null) {
                try {
                    this.skijaPaint.setPathEffect(PathEffect.makeDash(dashes, bs.getDashPhase()));
                } catch (RuntimeException re) {
                    System.err.println("Unable to create skija paint for dashes: "+Arrays.toString(dashes));
                    re.printStackTrace(System.err);
                    this.skijaPaint.setPathEffect(null);
                }
            } else {
                this.skijaPaint.setPathEffect(null);
            }
        }
        this.stroke = s;
    }

    /**
     * Maps a line cap code from AWT to the corresponding Skija {@code PaintStrokeCap}
     * enum value.
     * 
     * @param c  the line cap code.
     * 
     * @return A Skija stroke cap value.
     */
    private PaintStrokeCap awtToSkijaLineCap(int c) {
        switch (c) {
            case BasicStroke.CAP_BUTT:
                return PaintStrokeCap.BUTT;
            case BasicStroke.CAP_ROUND:
                return PaintStrokeCap.ROUND;
            case BasicStroke.CAP_SQUARE:
                return PaintStrokeCap.SQUARE;
            default:
                throw new IllegalArgumentException("Unrecognised cap code: " + c);
        }
    }

    /**
     * Maps a line join code from AWT to the corresponding Skija
     * {@code PaintStrokeJoin} enum value.
     * 
     * @param j  the line join code.
     * 
     * @return A Skija stroke join value.
     */
    private PaintStrokeJoin awtToSkijaLineJoin(int j) {
        switch (j) {
            case BasicStroke.JOIN_BEVEL:
                return PaintStrokeJoin.BEVEL;
            case BasicStroke.JOIN_MITER:
                return PaintStrokeJoin.MITER;
            case BasicStroke.JOIN_ROUND:
                return PaintStrokeJoin.ROUND;
            default:
                throw new IllegalArgumentException("Unrecognised join code: " + j);
        }
    }

    /**
     * Maps a linear gradient paint cycle method from AWT to the corresponding Skija
     * {@code FilterTileMode} enum value.
     *
     * @param method  the cycle method.
     *
     * @return A Skija stroke join value.
     */
    private FilterTileMode awtCycleMethodToSkijaFilterTileMode(MultipleGradientPaint.CycleMethod method) {
        switch (method) {
            case NO_CYCLE:
                return FilterTileMode.CLAMP;
            case REPEAT:
                return FilterTileMode.REPEAT;
            case REFLECT:
                return FilterTileMode.MIRROR;
            default:
                return FilterTileMode.CLAMP;
        }
    }

    /**
     * Returns the current value for the specified hint.  Note that all hints
     * are currently ignored in this implementation.
     * 
     * @param hintKey  the hint key ({@code null} permitted, but the
     *     result will be {@code null} also in that case).
     * 
     * @return The current value for the specified hint 
     *     (possibly {@code null}).
     * 
     * @see #setRenderingHint(java.awt.RenderingHints.Key, java.lang.Object) 
     */
    @Override
    public Object getRenderingHint(RenderingHints.Key hintKey) {
        if (LOG_ENABLED) {
            LOGGER.debug("getRenderingHint({})", hintKey);
        }
        return this.hints.get(hintKey);
    }

    /**
     * Sets the value for a hint.  See the {@code FXHints} class for
     * information about the hints that can be used with this implementation.
     * 
     * @param hintKey  the hint key ({@code null} not permitted).
     * @param hintValue  the hint value.
     * 
     * @see #getRenderingHint(java.awt.RenderingHints.Key) 
     */
    @Override
    public void setRenderingHint(RenderingHints.Key hintKey, Object hintValue) {
        if (LOG_ENABLED) {
            LOGGER.debug("setRenderingHint({}, {})", hintKey, hintValue);
        }
        this.hints.put(hintKey, hintValue);
    }

    /**
     * Sets the rendering hints to the specified collection.
     * 
     * @param hints  the new set of hints ({@code null} not permitted).
     * 
     * @see #getRenderingHints() 
     */
    @Override
    public void setRenderingHints(Map<?, ?> hints) {
        if (LOG_ENABLED) {
            LOGGER.debug("setRenderingHints(Map<?, ?>): {}", hints);
        }
        this.hints.clear();
        this.hints.putAll(hints);
    }

    /**
     * Adds all the supplied rendering hints.
     * 
     * @param hints  the hints ({@code null} not permitted).
     */
    @Override
    public void addRenderingHints(Map<?, ?> hints) {
        if (LOG_ENABLED) {
            LOGGER.debug("addRenderingHints(Map<?, ?>): {}", hints);
        }
        this.hints.putAll(hints);
    }

    /**
     * Returns a copy of the rendering hints.  Modifying the returned copy
     * will have no impact on the state of this {@code Graphics2D} 
     * instance.
     * 
     * @return The rendering hints (never {@code null}). 
     * 
     * @see #setRenderingHints(java.util.Map) 
     */
    @Override
    public RenderingHints getRenderingHints() {
        if (LOG_ENABLED) {
            LOGGER.debug("getRenderingHints()");
        }
        return (RenderingHints) this.hints.clone();
    }

    private RenderingHints getRenderingHintsInternally() {
        if (LOG_ENABLED) {
            LOGGER.debug("getRenderingHintsInternally()");
        }
        return this.hints;
    }

    /**
     * Applies the translation {@code (tx, ty)}.  This call is delegated 
     * to {@link #translate(double, double)}.
     * 
     * @param tx  the x-translation.
     * @param ty  the y-translation.
     * 
     * @see #translate(double, double) 
     */
    @Override
    public void translate(int tx, int ty) {
        if (LOG_ENABLED) {
            LOGGER.debug("translate({}, {})", tx, ty);
        }
        translate((double) tx, (double) ty);
    }

    /**
     * Applies the translation {@code (tx, ty)}.
     * 
     * @param tx  the x-translation.
     * @param ty  the y-translation.
     */
    @Override
    public void translate(double tx, double ty) {
        if (LOG_ENABLED) {
            LOGGER.debug("translate({}, {})", tx, ty);
        }
        this.transform.translate(tx, ty);
        this.canvas.translate((float) tx, (float) ty);
    }

    /**
     * Applies a rotation (anti-clockwise) about {@code (0, 0)}.
     * 
     * @param theta  the rotation angle (in radians). 
     */
    @Override
    public void rotate(double theta) {
        if (LOG_ENABLED) {
            LOGGER.debug("rotate({})", theta);
        }
        this.transform.rotate(theta);
        this.canvas.rotate((float) Math.toDegrees(theta));
    }

    /**
     * Applies a rotation (anti-clockwise) about {@code (x, y)}.
     * 
     * @param theta  the rotation angle (in radians).
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     */
    @Override
    public void rotate(double theta, double x, double y) {
        if (LOG_ENABLED) {
            LOGGER.debug("rotate({}, {}, {})", theta, x, y);
        }
        translate(x, y);
        rotate(theta);
        translate(-x, -y);
    }

    /**
     * Applies a scale transformation.
     * 
     * @param sx  the x-scaling factor.
     * @param sy  the y-scaling factor.
     */
    @Override
    public void scale(double sx, double sy) {
        if (LOG_ENABLED) {
            LOGGER.debug("scale({}, {})", sx, sy);
        }
        this.transform.scale(sx, sy);
        this.canvas.scale((float) sx, (float) sy);
    }

    /**
     * Applies a shear transformation. This is equivalent to the following 
     * call to the {@code transform} method:
     * <br><br>
     * <ul><li>
     * {@code transform(AffineTransform.getShearInstance(shx, shy));}
     * </ul>
     * 
     * @param shx  the x-shear factor.
     * @param shy  the y-shear factor.
     */
    @Override
    public void shear(double shx, double shy) {
        if (LOG_ENABLED) {
            LOGGER.debug("shear({}, {})", shx, shy);
        }
        this.transform.shear(shx, shy);
        this.canvas.skew((float) shx, (float) shy);
    }

    /**
     * Applies this transform to the existing transform by concatenating it.
     * 
     * @param t  the transform ({@code null} not permitted). 
     */
    @Override
    public void transform(AffineTransform t) {
        if (LOG_ENABLED) {
            LOGGER.debug("transform(AffineTransform) : {}", t);
        }
        AffineTransform tx = getTransform();
        tx.concatenate(t);
        setTransform(tx);
    }

    /**
     * Returns a copy of the current transform.
     * 
     * @return A copy of the current transform (never {@code null}).
     * 
     * @see #setTransform(java.awt.geom.AffineTransform) 
     */
    @Override
    public AffineTransform getTransform() {
        if (LOG_ENABLED) {
            LOGGER.debug("getTransform()");
        }
        return (AffineTransform) this.transform.clone();
    }

    private AffineTransform getTransformInternally() {
        if (LOG_ENABLED) {
            LOGGER.debug("getTransformInternally()");
        }
        return this.transform;
    }

    /**
     * Sets the transform.
     * 
     * @param t  the new transform ({@code null} permitted, resets to the
     *     identity transform).
     * 
     * @see #getTransform() 
     */
    @Override
    public void setTransform(AffineTransform t) {
        if (LOG_ENABLED) {
            LOGGER.debug("setTransform({})", t);
        }
        if (t == null) {
            this.transform = t = new AffineTransform();
        } else {
            this.transform = new AffineTransform(t);
        }
        Matrix33 m33 = new Matrix33((float) t.getScaleX(), (float) t.getShearX(), (float) t.getTranslateX(),
                (float) t.getShearY(), (float) t.getScaleY(), (float) t.getTranslateY(), 0f, 0f, 1f);
        this.canvas.setMatrix(m33);
    }

    @Override
    public Paint getPaint() {
        return this.awtPaint;
    }

    /**
     * Returns the current composite.
     * 
     * @return The current composite (never {@code null}).
     * 
     * @see #setComposite(java.awt.Composite) 
     */
    @Override
    public Composite getComposite() {
        return this.composite;
    }

    /**
     * Returns the background color (the default value is {@link Color#BLACK}).
     * This attribute is used by the {@link #clearRect(int, int, int, int)} 
     * method.
     * 
     * @return The background color (possibly {@code null}). 
     * 
     * @see #setBackground(java.awt.Color) 
     */
    @Override
    public Color getBackground() {
        return this.background;
    }

    /**
     * Sets the background color.  This attribute is used by the 
     * {@link #clearRect(int, int, int, int)} method.  The reference 
     * implementation allows {@code null} for the background color so
     * we allow that too (but for that case, the {@link #clearRect(int, int, int, int)} 
     * method will do nothing).
     * 
     * @param color  the color ({@code null} permitted).
     * 
     * @see #getBackground() 
     */
    @Override
    public void setBackground(Color color) {
        this.background = color;
    }

    /**
     * Returns the current stroke (this attribute is used when drawing shapes). 
     * 
     * @return The current stroke (never {@code null}). 
     * 
     * @see #setStroke(java.awt.Stroke) 
     */
    @Override
    public Stroke getStroke() {
        return this.stroke;
    }

    /**
     * Returns the font render context.
     * 
     * @return The font render context (never {@code null}).
     */
    @Override
    public FontRenderContext getFontRenderContext() {
        return this.fontRenderContext;
    }

    /**
     * Creates a new graphics object that is a copy of this graphics object.
     *
     * @return A new graphics object.
     */
    @Override
    public Graphics create() {
        if (LOG_ENABLED) {
            LOGGER.debug("create()");
        }
        // use special copy-constructor:
        return new SkijaGraphics2D(this);
    }

    @Override
    public Graphics create(int x, int y, int width, int height) {
        if (LOG_ENABLED) {
            LOGGER.debug("create({}, {}, {}, {})", x, y, width, height);
        }
        return super.create(x, y, width, height);
    }

    /**
     * Returns the foreground color.  This method exists for backwards
     * compatibility in AWT, you should use the {@link #getPaint()} method.
     * This attribute is updated by the {@link #setColor(java.awt.Color)}
     * method, and also by the {@link #setPaint(java.awt.Paint)} method if
     * a {@code Color} instance is passed to the method.
     *
     * @return The foreground color (never {@code null}).
     *
     * @see #getPaint()
     */
    @Override
    public Color getColor() {
        return this.color;
    }

    /**
     * Sets the foreground color.  This method exists for backwards 
     * compatibility in AWT, you should use the 
     * {@link #setPaint(java.awt.Paint)} method.
     * 
     * @param c  the color ({@code null} permitted but ignored). 
     * 
     * @see #setPaint(java.awt.Paint) 
     */
    @Override
    public void setColor(Color c) {
        if (LOG_ENABLED) {
            LOGGER.debug("setColor(Color) : {}", c);
        }
        if (c == null || c.equals(this.color)) {
            return;
        }
        this.color = c;
        setPaint(c);
    }

    /**
     * Not implemented - the method does nothing.
     */
    @Override
    public void setPaintMode() {
        // not implemented
    }

    /**
     * Not implemented - the method does nothing.
     */
    @Override
    public void setXORMode(Color c1) {
        // not implemented
    }

    /**
     * Returns the current font used for drawing text.
     * 
     * @return The current font (never {@code null}).
     * 
     * @see #setFont(java.awt.Font) 
     */
    @Override
    public Font getFont() {
        return this.awtFont;
    }

    private FontStyle awtFontStyleToSkijaFontStyle(int style) {
        switch (style) {
            case Font.PLAIN:
                return FontStyle.NORMAL;
            case Font.BOLD:
                return FontStyle.BOLD;
            case Font.ITALIC:
                return FontStyle.ITALIC;
            case Font.BOLD + Font.ITALIC:
                return FontStyle.BOLD_ITALIC;
            default:
                return FontStyle.NORMAL;
        }
    }

    /**
     * Sets the font to be used for drawing text.
     *
     * @param font  the font ({@code null} is permitted but ignored).
     *
     * @see #getFont()
     */
    @Override
    public void setFont(final Font font) {
        if (LOG_ENABLED) {
            LOGGER.debug("setFont({})", font);
        }
        if (font == null) {
            return;
        }
        this.awtFont = font;

        String fontName = font.getName();
        // check if there is a font name mapping to apply
        @SuppressWarnings("unchecked")
        final Function<String, String> fm = (Function<String, String>) getRenderingHint(SkijaHints.KEY_FONT_MAPPING_FUNCTION);
        if (fm != null) {
            final String mappedFontName = fm.apply(fontName);
            if (mappedFontName != null) {
                if (LOG_ENABLED) {
                    LOGGER.debug("Mapped font name is {}", mappedFontName);
                }
                fontName = mappedFontName;
            }
        }
        final FontStyle style = awtFontStyleToSkijaFontStyle(font.getStyle());
        final TypefaceKey key = new TypefaceKey(fontName, style);

        this.typeface = TYPEFACE_MAP.get(key);
        if (this.typeface == null) {
            if (LOG_ENABLED) {
                LOGGER.debug("Typeface.makeFromName({} style={})", fontName, style);
            }
            this.typeface = Typeface.makeFromName(fontName, style);
            TYPEFACE_MAP.put(key, this.typeface);
        }
        this.skijaFont = new io.github.humbleui.skija.Font(this.typeface, font.getSize());
    }

    /**
     * Returns the font metrics for the specified font.
     * 
     * @param f  the font.
     * 
     * @return The font metrics. 
     */
    @Override
    public FontMetrics getFontMetrics(Font f) {
        return new SkijaFontMetrics(this.skijaFont, this.awtFont);
    }

    /**
     * Returns the bounds of the user clipping region.
     * 
     * @return The clip bounds (possibly {@code null}). 
     * 
     * @see #getClip() 
     */
    @Override
    public Rectangle getClipBounds() {
        if (this.clip == null) {
            return null;
        }
        return getClipInternally().getBounds();
    }

    /**
     * Returns the user clipping region.  The initial default value is 
     * {@code null}.
     * 
     * @return The user clipping region (possibly {@code null}).
     * 
     * @see #setClip(java.awt.Shape)
     */
    @Override
    public Shape getClip() {
        if (LOG_ENABLED) {
            LOGGER.debug("getClip()");
        }
        if (this.clip == null) {
            return null;
        }
        return _inverseTransform(this.clip, true);
    }

    private Shape getClipInternally() {
        if (this.clip == null) {
            return null;
        }
        return _inverseTransform(this.clip, false);
    }

    /**
     * Clips to the intersection of the current clipping region and the 
     * specified rectangle.
     * 
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the width.
     * @param height  the height.
     */
    @Override
    public void clipRect(int x, int y, int width, int height) {
        if (LOG_ENABLED) {
            LOGGER.debug("clipRect({}, {}, {}, {})", x, y, width, height);
        }
        clip(rect(x, y, width, height));
    }

    /**
     * Sets the user clipping region to the specified rectangle.
     * 
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the width.
     * @param height  the height.
     * 
     * @see #getClip() 
     */
    @Override
    public void setClip(int x, int y, int width, int height) {
        if (LOG_ENABLED) {
            LOGGER.debug("setClip({}, {}, {}, {})", x, y, width, height);
        }
        setClip(rect(x, y, width, height));
    }

    /**
     * Sets the user clipping region.
     * 
     * @param shape  the new user clipping region ({@code null} permitted).
     * 
     * @see #getClip()
     */
    @Override
    public void setClip(final Shape shape) {
        setClip(shape, true);
    }
        
    private void setClip(final Shape shape, final boolean clone) {
        if (LOG_ENABLED) {
            LOGGER.debug("setClip({})", shape);
        }
        // a new clip is being set, so first restore the original clip (and save
        // it again for future restores)
        this.canvas.restoreToCount(this.restoreCount);
        this.restoreCount = this.canvas.save();
        // restoring the clip might also reset the transform, so reapply it
        setTransform(getTransform());
        // null is handled fine here...
        this.clip = _createTransformedShape(shape, clone); // device space
        // now apply on the Skija canvas
        if (shape != null) {
            this.canvas.clipPath(path(shape));
        }
    }

    /**
     * Clips to the intersection of the current clipping region and the
     * specified shape. 
     * 
     * According to the Oracle API specification, this method will accept a 
     * {@code null} argument, but there is an open bug report (since 2004) 
     * that suggests this is wrong:
     * <p>
     * <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6206189">
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6206189</a>
     * 
     * @param s  the clip shape ({@code null} not permitted). 
     */
    @Override
    public void clip(Shape s) {
        if (LOG_ENABLED) {
            LOGGER.debug("clip({})", s);
        }
        if (s instanceof Line2D) {
            s = s.getBounds2D();
        }
        if (this.clip == null) {
            setClip(s);
            return;
        }
        if (s == null) {
            throw new NullPointerException("clip(Shape): null argument.");
        }
        final Shape clipUser = getClipInternally();
        final Shape clipNew;
        if (!s.intersects(clipUser.getBounds2D())) {
            clipNew = new Rectangle2D.Double();
        } else {
            // note: Area class is very slow (especially for rectangles)
            final Area a1 = new Area(s);
            final Area a2 = new Area(clipUser);
            a1.intersect(a2);
            clipNew = new Path2D.Double(a1);
        }
        setClip(clipNew, false); // in user space
    }

    /**
     * Not yet implemented.
     * 
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the width of the area.
     * @param height  the height of the area.
     * @param dx  the delta x.
     * @param dy  the delta y.
     */
    @Override
    public void copyArea(int x, int y, int width, int height, int dx, int dy) {
        if (LOG_ENABLED) {
            LOGGER.debug("copyArea({}, {}, {}, {}, {}, {}) - NOT IMPLEMENTED", x, y, width, height, dx, dy);
        }
        // FIXME: implement this, low priority
    }

    /**
     * Draws a line from {@code (x1, y1)} to {@code (x2, y2)} using 
     * the current {@code paint} and {@code stroke}.
     * 
     * @param x1  the x-coordinate of the start point.
     * @param y1  the y-coordinate of the start point.
     * @param x2  the x-coordinate of the end point.
     * @param y2  the x-coordinate of the end point.
     */
    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        if (LOG_ENABLED) {
            LOGGER.debug("drawLine()");
        }
        if (this.line == null) {
            this.line = new Line2D.Double(x1, y1, x2, y2);
        } else {
            this.line.setLine(x1, y1, x2, y2);
        }
        draw(this.line);
    }

    /**
     * Fills the specified rectangle with the current {@code paint}.
     * 
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the rectangle width.
     * @param height  the rectangle height.
     */
    @Override
    public void fillRect(int x, int y, int width, int height) {
        if (LOG_ENABLED) {
            LOGGER.debug("fillRect({}, {}, {}, {})", x, y, width, height);
        }
        fill(rect(x, y, width, height));
    }

    /**
     * Clears the specified rectangle by filling it with the current 
     * background color.  If the background color is {@code null}, this
     * method will do nothing.
     * 
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the width.
     * @param height  the height.
     * 
     * @see #getBackground() 
     */
    @Override
    public void clearRect(int x, int y, int width, int height) {
        if (LOG_ENABLED) {
            LOGGER.debug("clearRect({}, {}, {}, {})", x, y, width, height);
        }
        if (getBackground() == null) {
            return;  // we can't do anything
        }
        Paint saved = getPaint();
        setPaint(getBackground());
        fillRect(x, y, width, height);
        setPaint(saved);
    }

    /**
     * Sets the attributes of the reusable {@link Rectangle2D} object that is
     * used by the {@link SkijaGraphics2D#drawRect(int, int, int, int)} and
     * {@link SkijaGraphics2D#fillRect(int, int, int, int)} methods.
     * 
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the width.
     * @param height  the height.
     * 
     * @return A rectangle (never {@code null}).
     */
    private Rectangle2D rect(int x, int y, int width, int height) {
        if (this.rect == null) {
            this.rect = new Rectangle2D.Double(x, y, width, height);
        } else {
            this.rect.setRect(x, y, width, height);
        }
        return this.rect;
    }

    /**
     * Draws a rectangle with rounded corners using the current 
     * {@code paint} and {@code stroke}.
     * 
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the width.
     * @param height  the height.
     * @param arcWidth  the arc-width.
     * @param arcHeight  the arc-height.
     * 
     * @see #fillRoundRect(int, int, int, int, int, int) 
     */
    @Override
    public void drawRoundRect(int x, int y, int width, int height,
                              int arcWidth, int arcHeight) {
        if (LOG_ENABLED) {
            LOGGER.debug("drawRoundRect({}, {}, {}, {}, {}, {})", x, y, width, height, arcWidth, arcHeight);
        }
        draw(roundRect(x, y, width, height, arcWidth, arcHeight));
    }

    /**
     * Fills a rectangle with rounded corners using the current {@code paint}.
     * 
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the width.
     * @param height  the height.
     * @param arcWidth  the arc-width.
     * @param arcHeight  the arc-height.
     * 
     * @see #drawRoundRect(int, int, int, int, int, int) 
     */
    @Override
    public void fillRoundRect(int x, int y, int width, int height,
                              int arcWidth, int arcHeight) {
        if (LOG_ENABLED) {
            LOGGER.debug("fillRoundRect({}, {}, {}, {}, {}, {})", x, y, width, height, arcWidth, arcHeight);
        }
        fill(roundRect(x, y, width, height, arcWidth, arcHeight));
    }

    /**
     * Sets the attributes of the reusable {@link RoundRectangle2D} object that
     * is used by the {@link #drawRoundRect(int, int, int, int, int, int)} and
     * {@link #fillRoundRect(int, int, int, int, int, int)} methods.
     * 
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the width.
     * @param height  the height.
     * @param arcWidth  the arc width.
     * @param arcHeight  the arc height.
     * 
     * @return A round rectangle (never {@code null}).
     */
    private RoundRectangle2D roundRect(int x, int y, int width, int height,
                                       int arcWidth, int arcHeight) {
        if (this.roundRect == null) {
            this.roundRect = new RoundRectangle2D.Double(x, y, width, height,
                    arcWidth, arcHeight);
        } else {
            this.roundRect.setRoundRect(x, y, width, height,
                    arcWidth, arcHeight);
        }
        return this.roundRect;
    }

    /**
     * Draws an oval framed by the rectangle {@code (x, y, width, height)}
     * using the current {@code paint} and {@code stroke}.
     * 
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the width.
     * @param height  the height.
     * 
     * @see #fillOval(int, int, int, int) 
     */
    @Override
    public void drawOval(int x, int y, int width, int height) {
        if (LOG_ENABLED) {
            LOGGER.debug("drawOval({}, {}, {}, {})", x, y, width, height);
        }
        draw(oval(x, y, width, height));
    }

    /**
     * Fills an oval framed by the rectangle {@code (x, y, width, height)}.
     * 
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the width.
     * @param height  the height.
     * 
     * @see #drawOval(int, int, int, int) 
     */
    @Override
    public void fillOval(int x, int y, int width, int height) {
        if (LOG_ENABLED) {
            LOGGER.debug("fillOval({}, {}, {}, {})", x, y, width, height);
        }
        fill(oval(x, y, width, height));
    }

    /**
     * Returns an {@link Ellipse2D} object that may be reused (so this instance
     * should be used for short term operations only). See the 
     * {@link #drawOval(int, int, int, int)} and 
     * {@link #fillOval(int, int, int, int)} methods for usage.
     * 
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the width.
     * @param height  the height.
     * 
     * @return An oval shape (never {@code null}).
     */
    private Ellipse2D oval(int x, int y, int width, int height) {
        if (this.oval == null) {
            this.oval = new Ellipse2D.Double(x, y, width, height);
        } else {
            this.oval.setFrame(x, y, width, height);
        }
        return this.oval;
    }

    /**
     * Draws an arc contained within the rectangle 
     * {@code (x, y, width, height)}, starting at {@code startAngle}
     * and continuing through {@code arcAngle} degrees using 
     * the current {@code paint} and {@code stroke}.
     * 
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the width.
     * @param height  the height.
     * @param startAngle  the start angle in degrees, 0 = 3 o'clock.
     * @param arcAngle  the angle (anticlockwise) in degrees.
     * 
     * @see #fillArc(int, int, int, int, int, int) 
     */
    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle,
                        int arcAngle) {
        if (LOG_ENABLED) {
            LOGGER.debug("drawArc({}, {}, {}, {}, {}, {})", x, y, width, height, startAngle, arcAngle);
        }
        draw(arc(x, y, width, height, startAngle, arcAngle));
    }

    /**
     * Fills an arc contained within the rectangle 
     * {@code (x, y, width, height)}, starting at {@code startAngle}
     * and continuing through {@code arcAngle} degrees, using 
     * the current {@code paint}.
     * 
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the width.
     * @param height  the height.
     * @param startAngle  the start angle in degrees, 0 = 3 o'clock.
     * @param arcAngle  the angle (anticlockwise) in degrees.
     * 
     * @see #drawArc(int, int, int, int, int, int) 
     */
    @Override
    public void fillArc(int x, int y, int width, int height, int startAngle,
                        int arcAngle) {
        if (LOG_ENABLED) {
            LOGGER.debug("fillArc({}, {}, {}, {}, {}, {})", x, y, width, height, startAngle, arcAngle);
        }
        fill(arc(x, y, width, height, startAngle, arcAngle));
    }

    /**
     * Sets the attributes of the reusable {@link Arc2D} object that is used by
     * {@link #drawArc(int, int, int, int, int, int)} and 
     * {@link #fillArc(int, int, int, int, int, int)} methods.
     * 
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the width.
     * @param height  the height.
     * @param startAngle  the start angle in degrees, 0 = 3 o'clock.
     * @param arcAngle  the angle (anticlockwise) in degrees.
     * 
     * @return An arc (never {@code null}).
     */
    private Arc2D arc(int x, int y, int width, int height, int startAngle,
                      int arcAngle) {
        if (this.arc == null) {
            this.arc = new Arc2D.Double(x, y, width, height, startAngle,
                    arcAngle, Arc2D.OPEN);
        } else {
            this.arc.setArc(x, y, width, height, startAngle, arcAngle,
                    Arc2D.OPEN);
        }
        return this.arc;
    }

    /**
     * Draws the specified multi-segment line using the current 
     * {@code paint} and {@code stroke}.
     * 
     * @param xPoints  the x-points.
     * @param yPoints  the y-points.
     * @param nPoints  the number of points to use for the polyline.
     */
    @Override
    public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
        if (LOG_ENABLED) {
            LOGGER.debug("drawPolyline(int[], int[], int)");
        }
        GeneralPath p = createPolygon(xPoints, yPoints, nPoints, false);
        draw(p);
    }

    /**
     * Draws the specified polygon using the current {@code paint} and 
     * {@code stroke}.
     * 
     * @param xPoints  the x-points.
     * @param yPoints  the y-points.
     * @param nPoints  the number of points to use for the polygon.
     * 
     * @see #fillPolygon(int[], int[], int)      */
    @Override
    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        if (LOG_ENABLED) {
            LOGGER.debug("drawPolygon(int[], int[], int)");
        }
        GeneralPath p = createPolygon(xPoints, yPoints, nPoints, true);
        draw(p);
    }

    /**
     * Fills the specified polygon using the current {@code paint}.
     * 
     * @param xPoints  the x-points.
     * @param yPoints  the y-points.
     * @param nPoints  the number of points to use for the polygon.
     * 
     * @see #drawPolygon(int[], int[], int) 
     */
    @Override
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        if (LOG_ENABLED) {
            LOGGER.debug("fillPolygon(int[], int[], int)");
        }
        GeneralPath p = createPolygon(xPoints, yPoints, nPoints, true);
        fill(p);
    }

    /**
     * Creates a polygon from the specified {@code x} and 
     * {@code y} coordinate arrays.
     * 
     * @param xPoints  the x-points.
     * @param yPoints  the y-points.
     * @param nPoints  the number of points to use for the polyline.
     * @param close  closed?
     * 
     * @return A polygon.
     */
    public GeneralPath createPolygon(int[] xPoints, int[] yPoints,
                                     int nPoints, boolean close) {
        if (LOG_ENABLED) {
            LOGGER.debug("createPolygon(int[], int[], int, boolean)");
        }
        GeneralPath p = new GeneralPath();
        p.moveTo(xPoints[0], yPoints[0]);
        for (int i = 1; i < nPoints; i++) {
            p.lineTo(xPoints[i], yPoints[i]);
        }
        if (close) {
            p.closePath();
        }
        return p;
    }

    /**
     * Draws an image at the location {@code (x, y)}.  Note that the 
     * {@code observer} is ignored.
     * 
     * @param img  the image ({@code null} permitted...method will do nothing).
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param observer  ignored.
     * 
     * @return {@code true} if there is no more drawing to be done. 
     */
    @Override
    public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
        if (LOG_ENABLED) {
            LOGGER.debug("drawImage(Image, {}, {}, ImageObserver)", x, y);
        }
        if (img == null) {
            return true;
        }
        int w = img.getWidth(observer);
        if (w < 0) {
            return false;
        }
        int h = img.getHeight(observer);
        if (h < 0) {
            return false;
        }
        return drawImage(img, x, y, w, h, observer);
    }

    /**
     * Draws the image into the rectangle defined by {@code (x, y, w, h)}.
     * Note that the {@code observer} is ignored (it is not useful in this
     * context).
     *
     * @param img  the image ({@code null} permitted...draws nothing).
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the width.
     * @param height  the height.
     * @param observer  ignored.
     *
     * @return {@code true} if there is no more drawing to be done.
     */
    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
        if (LOG_ENABLED) {
            LOGGER.debug("drawImage(Image, {}, {}, {}, {}, ImageObserver)", x, y, width, height);
        }
        // LOGGER.info("drawImage(Image, {}, {}, {}, {}, ImageObserver)", x, y, width, height);

        final BufferedImage buffered;
        if ((img instanceof BufferedImage) && ((BufferedImage) img).getType() == BufferedImage.TYPE_INT_ARGB) {
            buffered = (BufferedImage) img;
        } else {
            // LOGGER.info("drawImage(): copy BufferedImage");

            buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            final Graphics2D g2 = buffered.createGraphics();
            g2.drawImage(img, 0, 0, width, height, null);
            g2.dispose();
        }
        try ( io.github.humbleui.skija.Image skijaImage = convertToSkijaImage(buffered)) {
            this.canvas.drawImageRect(skijaImage, new Rect(x, y, x + width, y + height));
        }
        return true;
    }

    /**
     * Draws an image at the location {@code (x, y)}.  Note that the 
     * {@code observer} is ignored.
     * 
     * @param img  the image ({@code null} permitted...draws nothing).
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param bgcolor  the background color ({@code null} permitted).
     * @param observer  ignored.
     * 
     * @return {@code true} if there is no more drawing to be done. 
     */
    @Override
    public boolean drawImage(Image img, int x, int y, Color bgcolor,
                             ImageObserver observer) {
        if (LOG_ENABLED) {
            LOGGER.debug("drawImage(Image, {}, {}, Color, ImageObserver)", x, y);
        }
        if (img == null) {
            return true;
        }
        int w = img.getWidth(null);
        if (w < 0) {
            return false;
        }
        int h = img.getHeight(null);
        if (h < 0) {
            return false;
        }
        return drawImage(img, x, y, w, h, bgcolor, observer);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) {
        if (LOG_ENABLED) {
            LOGGER.debug("drawImage(Image, {}, {}, {}, {}, Color, ImageObserver)", x, y, width, height);
        }
        Paint saved = getPaint();
        setPaint(bgcolor);
        fillRect(x, y, width, height);
        setPaint(saved);
        return drawImage(img, x, y, width, height, observer);
    }

    /**
     * Draws part of an image (defined by the source rectangle
     * {@code (sx1, sy1, sx2, sy2)}) into the destination rectangle
     * {@code (dx1, dy1, dx2, dy2)}.  Note that the {@code observer}
     * is ignored in this implementation.
     *
     * @param img  the image.
     * @param dx1  the x-coordinate for the top left of the destination.
     * @param dy1  the y-coordinate for the top left of the destination.
     * @param dx2  the x-coordinate for the bottom right of the destination.
     * @param dy2  the y-coordinate for the bottom right of the destination.
     * @param sx1  the x-coordinate for the top left of the source.
     * @param sy1  the y-coordinate for the top left of the source.
     * @param sx2  the x-coordinate for the bottom right of the source.
     * @param sy2  the y-coordinate for the bottom right of the source.
     *
     * @return {@code true} if the image is drawn.
     */
    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, ImageObserver observer) {
        if (LOG_ENABLED) {
            LOGGER.debug("drawImage(Image, {}, {}, {}, {}, {}, {}, {}, {}, ImageObserver)", dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2);
        }
        int w = dx2 - dx1;
        int h = dy2 - dy1;
        BufferedImage img2 = new BufferedImage(w, h,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img2.createGraphics();
        g2.drawImage(img, 0, 0, w, h, sx1, sy1, sx2, sy2, null);
        return drawImage(img2, dx1, dy1, null);
    }

    /**
     * Draws part of an image (defined by the source rectangle
     * {@code (sx1, sy1, sx2, sy2)}) into the destination rectangle
     * {@code (dx1, dy1, dx2, dy2)}.  The destination rectangle is first
     * cleared by filling it with the specified {@code bgcolor}. Note that
     * the {@code observer} is ignored.
     *
     * @param img  the image.
     * @param dx1  the x-coordinate for the top left of the destination.
     * @param dy1  the y-coordinate for the top left of the destination.
     * @param dx2  the x-coordinate for the bottom right of the destination.
     * @param dy2  the y-coordinate for the bottom right of the destination.
     * @param sx1 the x-coordinate for the top left of the source.
     * @param sy1 the y-coordinate for the top left of the source.
     * @param sx2 the x-coordinate for the bottom right of the source.
     * @param sy2 the y-coordinate for the bottom right of the source.
     * @param bgcolor  the background color ({@code null} permitted).
     * @param observer  ignored.
     *
     * @return {@code true} if the image is drawn.
     */
    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color bgcolor, ImageObserver observer) {
        if (LOG_ENABLED) {
            LOGGER.debug("drawImage(Image, {}, {}, {}, {}, {}, {}, {}, {}, Color, ImageObserver)", dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2);
        }
        Paint saved = getPaint();
        setPaint(bgcolor);
        fillRect(dx1, dy1, dx2 - dx1, dy2 - dy1);
        setPaint(saved);
        return drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer);
    }

    /**
     * This method does nothing.
     */
    @Override
    public void dispose() {
        if (LOG_ENABLED) {
            LOGGER.debug("dispose()");
        }
        this.canvas.restoreToCount(this.restoreCount);
    }

    /**
     * Returns {@code true} if the two {@code Paint} objects are equal 
     * OR both {@code null}.  This method handles
     * {@code GradientPaint}, {@code LinearGradientPaint} 
     * and {@code RadialGradientPaint} as special cases, since those classes do
     * not override the {@code equals()} method.
     *
     * @param p1  paint 1 ({@code null} permitted).
     * @param p2  paint 2 ({@code null} permitted).
     *
     * @return A boolean.
     */
    private static boolean paintsAreEqual(Paint p1, Paint p2) {
        if (p1 == p2) {
            return true;
        }

        // handle cases where either or both arguments are null
        if (p1 == null) {
            return (p2 == null);
        }
        if (p2 == null) {
            return false;
        }

        // handle cases...
        if (p1 instanceof Color && p2 instanceof Color) {
            return p1.equals(p2);
        }
        if (p1 instanceof GradientPaint && p2 instanceof GradientPaint) {
            GradientPaint gp1 = (GradientPaint) p1;
            GradientPaint gp2 = (GradientPaint) p2;
            return gp1.getColor1().equals(gp2.getColor1())
                    && gp1.getColor2().equals(gp2.getColor2())
                    && gp1.getPoint1().equals(gp2.getPoint1())
                    && gp1.getPoint2().equals(gp2.getPoint2())
                    && gp1.isCyclic() == gp2.isCyclic()
                    && gp1.getTransparency() == gp1.getTransparency();
        }
        if (p1 instanceof LinearGradientPaint
                && p2 instanceof LinearGradientPaint) {
            LinearGradientPaint lgp1 = (LinearGradientPaint) p1;
            LinearGradientPaint lgp2 = (LinearGradientPaint) p2;
            return lgp1.getStartPoint().equals(lgp2.getStartPoint())
                    && lgp1.getEndPoint().equals(lgp2.getEndPoint())
                    && Arrays.equals(lgp1.getFractions(), lgp2.getFractions())
                    && Arrays.equals(lgp1.getColors(), lgp2.getColors())
                    && lgp1.getCycleMethod() == lgp2.getCycleMethod()
                    && lgp1.getColorSpace() == lgp2.getColorSpace()
                    && lgp1.getTransform().equals(lgp2.getTransform());
        }
        if (p1 instanceof RadialGradientPaint
                && p2 instanceof RadialGradientPaint) {
            RadialGradientPaint rgp1 = (RadialGradientPaint) p1;
            RadialGradientPaint rgp2 = (RadialGradientPaint) p2;
            return rgp1.getCenterPoint().equals(rgp2.getCenterPoint())
                    && rgp1.getRadius() == rgp2.getRadius()
                    && rgp1.getFocusPoint().equals(rgp2.getFocusPoint())
                    && Arrays.equals(rgp1.getFractions(), rgp2.getFractions())
                    && Arrays.equals(rgp1.getColors(), rgp2.getColors())
                    && rgp1.getCycleMethod() == rgp2.getCycleMethod()
                    && rgp1.getColorSpace() == rgp2.getColorSpace()
                    && rgp1.getTransform().equals(rgp2.getTransform());
        }
        return p1.equals(p2);
    }

    /**
     * Converts a rendered image to a {@code BufferedImage}.  This utility
     * method has come from a forum post by Jim Moore at:
     * <p>
     * <a href="http://www.jguru.com/faq/view.jsp?EID=114602">
     * http://www.jguru.com/faq/view.jsp?EID=114602</a>
     * 
     * @param img  the rendered image.
     * 
     * @return A buffered image. 
     */
    @SuppressWarnings("unchecked")
    private static BufferedImage convertRenderedImage(RenderedImage img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }
        final int width = img.getWidth();
        final int height = img.getHeight();
        final ColorModel cm = img.getColorModel();
        final boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();

        final WritableRaster raster = cm.createCompatibleWritableRaster(width, height);

        final Hashtable properties = new Hashtable();
        String[] keys = img.getPropertyNames();
        if (keys != null) {
            for (String key : keys) {
                properties.put(key, img.getProperty(key));
            }
        }
        final BufferedImage result = new BufferedImage(cm, raster, isAlphaPremultiplied, properties);
        img.copyData(raster);
        return result;
    }

    private static io.github.humbleui.skija.Image convertToSkijaImage(final BufferedImage image) {
        // TODO: monitor performance:
        final int w = image.getWidth();
        final int h = image.getHeight();

        final DataBufferInt db = (DataBufferInt) image.getRaster().getDataBuffer();
        final int[] pixels = db.getData();

        final byte[] bytes = new byte[pixels.length * 4]; // Big alloc!

        for (int i = 0; i < pixels.length; i++) {
            final int p = pixels[i];
            bytes[i * 4 + 3] = (byte) ((p & 0xFF000000) >> 24);
            bytes[i * 4 + 2] = (byte) ((p & 0xFF0000) >> 16);
            bytes[i * 4 + 1] = (byte) ((p & 0xFF00) >> 8);
            bytes[i * 4] = (byte) (p & 0xFF);
        }
        final ImageInfo imageInfo = new ImageInfo(w, h, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL);

        // LOGGER.info("convertToSkijaImage(): {}", imageInfo);
        return io.github.humbleui.skija.Image.makeRaster(imageInfo, bytes, 4L * w);
    }

    private Shape _createTransformedShape(final Shape s, final boolean clone) {
        if (this.transform.isIdentity()) {
            return (clone) ? _clone(s) : s;
        }
        return this.transform.createTransformedShape(s);
    }

    private Shape _inverseTransform(final Shape s, final boolean clone) {
        if (this.transform.isIdentity()) {
            return (clone) ? _clone(s) : s;
        }
        try {
            final AffineTransform inv = this.transform.createInverse();
            return inv.createTransformedShape(s);
        } catch (NoninvertibleTransformException nite) {
            // System.err.println("NoninvertibleTransformException: " + this.transform);
            return null;
        }
    }
    
    private Shape _clone(final Shape s) {
        if (s == null) {
            return null;
        }
        if (s instanceof Rectangle2D) {
            final Rectangle2D r = new Rectangle2D.Double();
            r.setRect((Rectangle2D)s);
            return r;
        }
        return new Path2D.Double(s, null);
    }
}
