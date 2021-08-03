package org.jfree.skija;

import java.awt.*;

/**
 * A graphics device for SkijaGraphics2D.
 */
public class SkijaGraphicsDevice extends GraphicsDevice {

    private final String id;

    GraphicsConfiguration defaultConfig;

    /**
     * Creates a new instance.
     *
     * @param id  the id.
     * @param defaultConfig  the default configuration.
     */
    public SkijaGraphicsDevice(String id, GraphicsConfiguration defaultConfig) {
        this.id = id;
        this.defaultConfig = defaultConfig;
    }

    /**
     * Returns the device type.
     *
     * @return The device type.
     */
    @Override
    public int getType() {
        return GraphicsDevice.TYPE_RASTER_SCREEN;
    }

    /**
     * Returns the id string (defined in the constructor).
     *
     * @return The id string.
     */
    @Override
    public String getIDstring() {
        return this.id;
    }

    /**
     * Returns all configurations for this device.
     *
     * @return All configurations for this device.
     */
    @Override
    public GraphicsConfiguration[] getConfigurations() {
        return new GraphicsConfiguration[] { getDefaultConfiguration() };
    }

    /**
     * Returns the default configuration for this device.
     *
     * @return The default configuration for this device.
     */
    @Override
    public GraphicsConfiguration getDefaultConfiguration() {
        return this.defaultConfig;
    }

}