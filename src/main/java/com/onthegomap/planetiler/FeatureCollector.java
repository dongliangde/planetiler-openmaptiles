//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.onthegomap.planetiler;

import com.onthegomap.planetiler.config.PlanetilerConfig;
import com.onthegomap.planetiler.geo.GeoUtils;
import com.onthegomap.planetiler.geo.GeometryException;
import com.onthegomap.planetiler.geo.GeometryType;
import com.onthegomap.planetiler.reader.SourceFeature;
import com.onthegomap.planetiler.stats.Stats;
import com.onthegomap.planetiler.util.CacheByZoom;
import com.onthegomap.planetiler.util.ZoomFunction;

import java.util.*;

import org.locationtech.jts.geom.Geometry;

public class FeatureCollector implements Iterable<FeatureCollector.Feature> {
    private static final Geometry EMPTY_GEOM;
    private final SourceFeature source;
    private final List<Feature> output = new ArrayList();
    private final PlanetilerConfig config;
    private final Stats stats;

    private FeatureCollector(SourceFeature source, PlanetilerConfig config, Stats stats) {
        this.source = source;
        this.config = config;
        this.stats = stats;
    }

    public Iterator<Feature> iterator() {
        return this.output.iterator();
    }

    public Feature geometry(String layer, Geometry geometry) {
        Feature feature = new Feature(layer, geometry, this.source.id());
        this.output.add(feature);
        return feature;
    }

    public Feature point(String layer) {
        try {
            if (!this.source.isPoint()) {
                throw new GeometryException("feature_not_point", "not a point");
            } else {
                return this.geometry(layer, this.source.worldGeometry());
            }
        } catch (GeometryException var3) {
            var3.log(this.stats, "feature_point", "Error getting point geometry for " + this.source.id());
            return new Feature(layer, EMPTY_GEOM, this.source.id());
        }
    }

    public Feature line(String layer) {
        try {
            return this.geometry(layer, this.source.line());
        } catch (GeometryException var3) {
            var3.log(this.stats, "feature_line", "Error constructing line for " + this.source.id());
            return new Feature(layer, EMPTY_GEOM, this.source.id());
        }
    }

    public Feature polygon(String layer) {
        try {
            return this.geometry(layer, this.source.polygon());
        } catch (GeometryException var3) {
            var3.log(this.stats, "feature_polygon", "Error constructing polygon for " + this.source.id());
            return new Feature(layer, EMPTY_GEOM, this.source.id());
        }
    }

    public Feature centroid(String layer) {
        try {
            return this.geometry(layer, this.source.centroid());
        } catch (GeometryException var3) {
            var3.log(this.stats, "feature_centroid", "Error getting centroid for " + this.source.id());
            return new Feature(layer, EMPTY_GEOM, this.source.id());
        }
    }

    public Feature centroidIfConvex(String layer) {
        try {
            return this.geometry(layer, this.source.centroidIfConvex());
        } catch (GeometryException var3) {
            var3.log(this.stats, "feature_centroid_if_convex", "Error constructing centroid if convex for " + this.source.id());
            return new Feature(layer, EMPTY_GEOM, this.source.id());
        }
    }

    public Feature pointOnSurface(String layer) {
        try {
            return this.geometry(layer, this.source.pointOnSurface());
        } catch (GeometryException var3) {
            var3.log(this.stats, "feature_point_on_surface", "Error constructing point on surface for " + this.source.id());
            return new Feature(layer, EMPTY_GEOM, this.source.id());
        }
    }

    static {
        EMPTY_GEOM = GeoUtils.JTS_FACTORY.createGeometryCollection();
    }

    public final class Feature {
        private static final double DEFAULT_LABEL_GRID_SIZE = 0.0;
        private static final int DEFAULT_LABEL_GRID_LIMIT = 0;
        private final String layer;
        private final Geometry geom;
        private final Map<String, Object> attrs = new TreeMap();
        private final GeometryType geometryType;
        private final long sourceId;
        private int sortKey = 0;
        private int minzoom;
        private int maxzoom;
        private ZoomFunction<Number> labelGridPixelSize;
        private ZoomFunction<Number> labelGridLimit;
        private boolean attrsChangeByZoom;
        private CacheByZoom<Map<String, Object>> attrCache;
        private double defaultBufferPixels;
        private ZoomFunction<Number> bufferPixelOverrides;
        private double defaultMinPixelSize;
        private double minPixelSizeAtMaxZoom;
        private ZoomFunction<Number> minPixelSize;
        private double defaultPixelTolerance;
        private double pixelToleranceAtMaxZoom;
        private ZoomFunction<Number> pixelTolerance;
        private String numPointsAttr;

        private Feature(String layer, Geometry geom, long sourceId) {
            this.minzoom = FeatureCollector.this.config.minzoom();
            this.maxzoom = FeatureCollector.this.config.maxzoom();
            this.labelGridPixelSize = null;
            this.labelGridLimit = null;
            this.attrsChangeByZoom = false;
            this.attrCache = null;
            this.defaultBufferPixels = 4.0;
            this.defaultMinPixelSize = FeatureCollector.this.config.minFeatureSizeBelowMaxZoom();
            this.minPixelSizeAtMaxZoom = FeatureCollector.this.config.minFeatureSizeAtMaxZoom();
            this.minPixelSize = null;
            this.defaultPixelTolerance = FeatureCollector.this.config.simplifyToleranceBelowMaxZoom();
            this.pixelToleranceAtMaxZoom = FeatureCollector.this.config.simplifyToleranceAtMaxZoom();
            this.pixelTolerance = null;
            this.numPointsAttr = null;
            this.layer = layer;
            this.geom = geom;
            this.geometryType = GeometryType.typeOf(geom);
            this.sourceId = sourceId;
        }

        public long getSourceId() {
            return this.sourceId;
        }

        GeometryType getGeometryType() {
            return this.geometryType;
        }

        public boolean isPolygon() {
            return this.geometryType == GeometryType.POLYGON;
        }

        public int getSortKey() {
            return this.sortKey;
        }

        public Feature setSortKey(int sortKey) {
            assert sortKey >= -4194304 && sortKey <= 4194303 : "Sort key " + sortKey + " outside of allowed range [-4194304, 4194303]";

            this.sortKey = sortKey;
            return this;
        }

        public Feature setSortKeyDescending(int sortKey) {
            return this.setSortKey(-1 - sortKey);
        }

        public Feature setZoomRange(int min, int max) {
            assert min <= max;

            return this.setMinZoom(min).setMaxZoom(max);
        }

        public int getMinZoom() {
            return this.minzoom;
        }

        public Feature setMinZoom(int min) {
            this.minzoom = Math.max(min, FeatureCollector.this.config.minzoom());
            return this;
        }

        public int getMaxZoom() {
            return this.maxzoom;
        }

        public Feature setMaxZoom(int max) {
            this.maxzoom = Math.min(max, FeatureCollector.this.config.maxzoom());
            return this;
        }

        public String getLayer() {
            return this.layer;
        }

        public Geometry getGeometry() {
            return this.geom;
        }

        public double getBufferPixelsAtZoom(int zoom) {
            return ZoomFunction.applyAsDoubleOrElse(this.bufferPixelOverrides, zoom, this.defaultBufferPixels);
        }

        public Feature setBufferPixels(double buffer) {
            this.defaultBufferPixels = buffer;
            return this;
        }

        public Feature setBufferPixelOverrides(ZoomFunction<Number> buffer) {
            this.bufferPixelOverrides = buffer;
            return this;
        }

        public double getMinPixelSizeAtZoom(int zoom) {
            return zoom == FeatureCollector.this.config.maxzoomForRendering() ? this.minPixelSizeAtMaxZoom : ZoomFunction.applyAsDoubleOrElse(this.minPixelSize, zoom, this.defaultMinPixelSize);
        }

        public Feature setMinPixelSize(double minPixelSize) {
            this.defaultMinPixelSize = minPixelSize;
            return this;
        }

        public Feature setMinPixelSizeOverrides(ZoomFunction<Number> levels) {
            this.minPixelSize = levels;
            return this;
        }

        public Feature setMinPixelSizeBelowZoom(int zoom, double minPixelSize) {
            if (zoom >= FeatureCollector.this.config.maxzoomForRendering()) {
                this.minPixelSizeAtMaxZoom = minPixelSize;
            }

            this.minPixelSize = ZoomFunction.maxZoom(zoom, minPixelSize);
            return this;
        }

        public Feature setMinPixelSizeAtMaxZoom(double minPixelSize) {
            this.minPixelSizeAtMaxZoom = minPixelSize;
            return this;
        }

        public Feature setMinPixelSizeAtAllZooms(int minPixelSize) {
            this.minPixelSizeAtMaxZoom = (double)minPixelSize;
            return this.setMinPixelSize((double)minPixelSize);
        }

        public double getPixelToleranceAtZoom(int zoom) {
            return zoom == FeatureCollector.this.config.maxzoomForRendering() ? this.pixelToleranceAtMaxZoom : ZoomFunction.applyAsDoubleOrElse(this.pixelTolerance, zoom, this.defaultPixelTolerance);
        }

        public Feature setPixelTolerance(double tolerance) {
            this.defaultPixelTolerance = tolerance;
            return this;
        }

        public Feature setPixelToleranceAtMaxZoom(double tolerance) {
            this.pixelToleranceAtMaxZoom = tolerance;
            return this;
        }

        public Feature setPixelToleranceAtAllZooms(double tolerance) {
            return this.setPixelToleranceAtMaxZoom(tolerance).setPixelTolerance(tolerance);
        }

        public Feature setPixelToleranceOverrides(ZoomFunction<Number> overrides) {
            this.pixelTolerance = overrides;
            return this;
        }

        public Feature setPixelToleranceBelowZoom(int zoom, double tolerance) {
            if (zoom == FeatureCollector.this.config.maxzoomForRendering()) {
                this.pixelToleranceAtMaxZoom = tolerance;
            }

            return this.setPixelToleranceOverrides(ZoomFunction.maxZoom(zoom, tolerance));
        }

        public boolean hasLabelGrid() {
            return this.labelGridPixelSize != null || this.labelGridLimit != null;
        }

        public double getPointLabelGridPixelSizeAtZoom(int zoom) {
            double result = ZoomFunction.applyAsDoubleOrElse(this.labelGridPixelSize, zoom, 0.0);

            assert result <= this.getBufferPixelsAtZoom(zoom) : "to avoid inconsistent rendering of the same point between adjacent tiles, buffer pixel size should be >= label grid size but in '%s' buffer pixel size=%f was greater than label grid size=%f".formatted(new Object[]{this.getLayer(), this.getBufferPixelsAtZoom(zoom), result});

            return result;
        }

        public int getPointLabelGridLimitAtZoom(int zoom) {
            return ZoomFunction.applyAsIntOrElse(this.labelGridLimit, zoom, 0);
        }

        public Feature setPointLabelGridPixelSize(ZoomFunction<Number> labelGridSize) {
            this.labelGridPixelSize = labelGridSize;
            return this;
        }

        public Feature setPointLabelGridPixelSize(int maxzoom, double size) {
            return this.setPointLabelGridPixelSize(ZoomFunction.maxZoom(maxzoom, size));
        }

        public Feature setPointLabelGridLimit(ZoomFunction<Number> labelGridLimit) {
            this.labelGridLimit = labelGridLimit;
            return this;
        }

        public Feature setPointLabelGridSizeAndLimit(int maxzoom, double size, int limit) {
            return this.setPointLabelGridPixelSize(ZoomFunction.maxZoom(maxzoom, size)).setPointLabelGridLimit(ZoomFunction.maxZoom(maxzoom, limit));
        }

        private Map<String, Object> computeAttrsAtZoom(int zoom) {
            Map<String, Object> result = new TreeMap();
            Iterator var3 = this.attrs.entrySet().iterator();

            while(var3.hasNext()) {
                Map.Entry<String, Object> entry = (Map.Entry)var3.next();
                Object value = entry.getValue();
                if (value instanceof ZoomFunction<?> fn) {
                    value = fn.apply(zoom);
                }

                if (value != null && !"".equals(value)) {
                    result.put((String)entry.getKey(), value);
                }
            }

            return result;
        }

        public Map<String, Object> getAttrsAtZoom(int zoom) {
            if (!this.attrsChangeByZoom) {
                return this.attrs;
            } else {
                if (this.attrCache == null) {
                    this.attrCache = CacheByZoom.create(this::computeAttrsAtZoom);
                }

                return (Map)this.attrCache.get(zoom);
            }
        }

        public Feature inheritAttrFromSource(String key) {
            return this.setAttr(key, FeatureCollector.this.source.getTag(key));
        }

        public Feature setAttr(String key, Object value) {
            if (value instanceof ZoomFunction) {
                this.attrsChangeByZoom = true;
            }

            if (value != null) {
                this.attrs.put(key, value);
            }

            return this;
        }

        /**
         * 2022年12月6日
         * 编译源码构建多动态方法支持批量操作
         * */
        public Feature setAttr(Map<String, Object> keys) {
            for (String key : keys.keySet()) {
                Object value = keys.get(key);
                if (keys.get(key) instanceof ZoomFunction) {
                    this.attrsChangeByZoom = true;
                }
                if (value != null) {
                    this.attrs.put(key, value);
                }
            }
            return this;
        }

        public Feature setAttrWithMinzoom(String key, Object value, int minzoom) {
            return this.setAttr(key, ZoomFunction.minZoom(minzoom, value));
        }

        public Feature putAttrsWithMinzoom(Map<String, Object> attrs, int minzoom) {
            Iterator var3 = attrs.entrySet().iterator();

            while(var3.hasNext()) {
                Map.Entry<String, Object> entry = (Map.Entry)var3.next();
                this.setAttrWithMinzoom((String)entry.getKey(), entry.getValue(), minzoom);
            }

            return this;
        }

        public Feature putAttrs(Map<String, Object> attrs) {
            Iterator var2 = attrs.values().iterator();

            while(var2.hasNext()) {
                Object value = var2.next();
                if (value instanceof ZoomFunction) {
                    this.attrsChangeByZoom = true;
                    break;
                }
            }

            this.attrs.putAll(attrs);
            return this;
        }

        public Feature setNumPointsAttr(String numPointsAttr) {
            this.numPointsAttr = numPointsAttr;
            return this;
        }

        public String getNumPointsAttr() {
            return this.numPointsAttr;
        }

        public String toString() {
            String var10000 = this.layer;
            return "Feature{layer='" + var10000 + "', geom=" + this.geom.getGeometryType() + ", attrs=" + this.attrs + "}";
        }
    }

    public static record Factory(PlanetilerConfig config, Stats stats) {
        public Factory(PlanetilerConfig config, Stats stats) {
            this.config = config;
            this.stats = stats;
        }

        public FeatureCollector get(SourceFeature source) {
            return new FeatureCollector(source, this.config, this.stats);
        }

        public PlanetilerConfig config() {
            return this.config;
        }

        public Stats stats() {
            return this.stats;
        }
    }
}
