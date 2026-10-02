package net.grid.whisperinganchors.client.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.RenderTypeGroup;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.Reader;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class FreeAngleModel {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final FaceBakery FACE_BAKERY = new FaceBakery();
    private static final ModelState IDENTITY = new FixedRotation(Transformation.identity());

    private static final float DEGREES_TO_RADIANS = (float) (Math.PI / 180.0);
    private static final int MAX_TEXTURE_INDIRECTION = 8;

    private FreeAngleModel() {
    }

    @Nullable
    public static BakedModel bake(ResourceManager resources, Function<Material, TextureAtlasSprite> sprites, ResourceLocation modelId) {
        ResourceLocation file = ResourceLocation.fromNamespaceAndPath(modelId.getNamespace(), "models/" + modelId.getPath() + ".json");

        try {
            JsonObject root;
            try (Reader reader = resources.getResourceOrThrow(file).openAsReader()) {
                root = GsonHelper.parse(reader);
            }

            Map<String, String> textures = new HashMap<>();
            if (root.has("textures")) {
                for (Map.Entry<String, JsonElement> entry : GsonHelper.getAsJsonObject(root, "textures").entrySet()) {
                    textures.put(entry.getKey(), entry.getValue().getAsString());
                }
            }

            Set<String> unresolved = new LinkedHashSet<>();
            List<BakedQuad> quads = new ArrayList<>();
            int freeAngled = 0;
            var elements = GsonHelper.getAsJsonArray(root, "elements");
            for (JsonElement element : elements) {
                if (bakeElement(element.getAsJsonObject(), textures, sprites, unresolved, quads)) {
                    freeAngled++;
                }
            }

            LOGGER.debug("Baked {} into {} quads, {} of {} elements free-angled", file, quads.size(), freeAngled, elements.size());
            if (!unresolved.isEmpty()) {
                LOGGER.warn("{}: textures not on the block atlas: {}", file, unresolved);
            }

            Map<Direction, List<BakedQuad>> culled = new EnumMap<>(Direction.class);
            for (Direction direction : Direction.values()) {
                culled.put(direction, List.of());
            }

            return new SimpleBakedModel(quads, culled, true, true, true, resolveSprite(sprites, textures, "#particle", unresolved), ItemTransforms.NO_TRANSFORMS, ItemOverrides.EMPTY, RenderTypeGroup.EMPTY);
        } catch (Exception exception) {
            LOGGER.error("Could not bake {}", file, exception);
            return null;
        }
    }

    private static boolean bakeElement(JsonObject json, Map<String, String> textures, Function<Material, TextureAtlasSprite> sprites, Set<String> unresolved, List<BakedQuad> out) {
        Vector3f from = readVector(json, "from");
        Vector3f to = readVector(json, "to");
        boolean shade = GsonHelper.getAsBoolean(json, "shade", true);

        Map<Direction, BlockElementFace> faces = new EnumMap<>(Direction.class);
        for (Map.Entry<String, JsonElement> entry : GsonHelper.getAsJsonObject(json, "faces").entrySet()) {
            Direction direction = Direction.byName(entry.getKey().toLowerCase(Locale.ROOT));
            if (direction == null) {
                throw new JsonParseException("Unknown face " + entry.getKey());
            }
            JsonObject face = entry.getValue().getAsJsonObject();
            String cull = GsonHelper.getAsString(face, "cullface", null);
            faces.put(direction, new BlockElementFace(cull == null ? null : Direction.byName(cull.toLowerCase(Locale.ROOT)), GsonHelper.getAsInt(face, "tintindex", -1), GsonHelper.getAsString(face, "texture"), new BlockFaceUV(face.has("uv") ? readFloats(face, "uv", 4) : null, GsonHelper.getAsInt(face, "rotation", 0))));
        }

        BlockElement element = new BlockElement(from, to, faces, null, shade);

        ElementRotation rotation = readRotation(json);
        ModelState state = rotation == null ? IDENTITY : new FixedRotation(rotation.toTransformation());
        BlockElementRotation keepWinding = rotation == null ? null : new BlockElementRotation(new Vector3f(), Direction.Axis.Y, 0.0F, false);

        for (Map.Entry<Direction, BlockElementFace> entry : element.faces.entrySet()) {
            BlockElementFace face = entry.getValue();
            out.add(FACE_BAKERY.bakeQuad(element.from, element.to, face, resolveSprite(sprites, textures, face.texture(), unresolved), entry.getKey(), state, keepWinding, element.shade));
        }

        return rotation != null && !rotation.isVanillaLegal();
    }

    @Nullable
    private static ElementRotation readRotation(JsonObject element) {
        if (!element.has("rotation")) return null;

        JsonObject json = GsonHelper.getAsJsonObject(element, "rotation");

        float x = 0.0F;
        float y = 0.0F;
        float z = 0.0F;

        if (json.has("axis")) {
            float angle = GsonHelper.getAsFloat(json, "angle", 0.0F);
            switch (GsonHelper.getAsString(json, "axis").toLowerCase(Locale.ROOT)) {
                case "x" -> x = angle;
                case "y" -> y = angle;
                case "z" -> z = angle;
                default -> throw new JsonParseException("Unknown rotation axis in " + json);
            }
        } else {
            x = GsonHelper.getAsFloat(json, "x", 0.0F);
            y = GsonHelper.getAsFloat(json, "y", 0.0F);
            z = GsonHelper.getAsFloat(json, "z", 0.0F);
        }

        if (x == 0.0F && y == 0.0F && z == 0.0F) return null;

        return new ElementRotation(readVector(json, "origin"), x, y, z, GsonHelper.getAsBoolean(json, "rescale", false));
    }

    private record ElementRotation(Vector3f origin, float x, float y, float z, boolean rescale) {

        Transformation toTransformation() {
            float ox = this.origin.x() / 16.0F - 0.5F;
            float oy = this.origin.y() / 16.0F - 0.5F;
            float oz = this.origin.z() / 16.0F - 0.5F;

            Matrix4f matrix = new Matrix4f().translate(ox, oy, oz);
            if (this.rescale && isSingleAxis()) {
                float scale = 1.0F / (float) Math.cos(getDominantAngle() * DEGREES_TO_RADIANS);
                matrix.scale(this.x == 0.0F ? scale : 1.0F, this.y == 0.0F ? scale : 1.0F, this.z == 0.0F ? scale : 1.0F);
            }

            matrix.rotateXYZ(this.x * DEGREES_TO_RADIANS, this.y * DEGREES_TO_RADIANS, this.z * DEGREES_TO_RADIANS);
            matrix.translate(-ox, -oy, -oz);

            return new Transformation(matrix);
        }

        boolean isVanillaLegal() {
            if (!isSingleAxis()) return false;

            float angle = Math.abs(getDominantAngle());

            return angle == 22.5F || angle == 45.0F;
        }

        private boolean isSingleAxis() {
            int axes = (this.x == 0.0F ? 0 : 1) + (this.y == 0.0F ? 0 : 1) + (this.z == 0.0F ? 0 : 1);
            return axes == 1;
        }

        private float getDominantAngle() {
            float angle = Math.abs(this.x) >= Math.abs(this.y) ? this.x : this.y;
            return Math.abs(angle) >= Math.abs(this.z) ? angle : this.z;
        }
    }

    private record FixedRotation(Transformation rotation) implements ModelState {

        @Override
        public Transformation getRotation() {
            return this.rotation;
        }
    }

    private static TextureAtlasSprite resolveSprite(Function<Material, TextureAtlasSprite> sprites, Map<String, String> textures, String reference, Set<String> unresolved) {
        String value = reference;
        for (int i = 0; i < MAX_TEXTURE_INDIRECTION && value != null && value.startsWith("#"); i++) {
            value = textures.get(value.substring(1));
        }

        ResourceLocation location = value == null || value.startsWith("#") ? null : ResourceLocation.tryParse(value);
        if (location == null) {
            unresolved.add(reference);
            location = MissingTextureAtlasSprite.getLocation();
        }

        TextureAtlasSprite sprite = sprites.apply(new Material(InventoryMenu.BLOCK_ATLAS, location));
        if (sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
            unresolved.add(location.toString());
        }

        return sprite;
    }

    private static Vector3f readVector(JsonObject json, String key) {
        float[] values = readFloats(json, key, 3);
        return new Vector3f(values[0], values[1], values[2]);
    }

    private static float[] readFloats(JsonObject json, String key, int expected) {
        var array = GsonHelper.getAsJsonArray(json, key);
        if (array.size() != expected) {
            throw new JsonParseException("Expected " + expected + " values in " + key + ", found " + array.size());
        }

        float[] values = new float[expected];
        for (int i = 0; i < expected; i++) {
            values[i] = GsonHelper.convertToFloat(array.get(i), key + "[" + i + "]");
        }

        return values;
    }
}
