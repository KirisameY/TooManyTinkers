package com.kirisamey.toomanytinkers.models;

import com.google.gson.*;
import com.ibm.icu.impl.Pair;
import com.kirisamey.toomanytinkers.TmtRegistries;
import com.kirisamey.toomanytinkers.TooManyTinkers;
import com.kirisamey.toomanytinkers.rendering.TmtRenderTypeGetters;
import lombok.extern.log4j.Log4j2;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

@Log4j2
public class AnimatableTicTool3DModelLoader implements IGeometryLoader<AnimatableTicTool3DUnbakedModel> {
    @Override
    public AnimatableTicTool3DUnbakedModel read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) throws JsonParseException {
        try {
            var partsJson = jsonObject.getAsJsonObject("parts");
            var parts = partsJson.entrySet().stream().map(e -> {
                var id = e.getKey();
                var info = e.getValue().getAsJsonObject();
                var model = Objects.requireNonNull(ResourceLocation.tryParse(info.get("model").getAsString()));
                var renderType = info.has("render_type") ?
                        TmtRegistries.RENDER_TYPE_GETTERS.get().getValue(
                                ResourceLocation.parse(info.get("render_type").getAsString())
                        ) : TmtRenderTypeGetters.TINKER_MAPPING.get();
                var toolPart = info.has("tool_part") ? info.get("tool_part").getAsInt() : -1;

                var origin = new Vector3f();
                if (info.has("origin")) {
                    var vecArray = info.getAsJsonArray("origin");
                    origin = new Vector3f(
                            vecArray.get(0).getAsFloat(),
                            vecArray.get(1).getAsFloat(),
                            vecArray.get(2).getAsFloat()
                    );
                }

                var offset = new Vector3f();
                if (info.has("offset")) {
                    var vecArray = info.getAsJsonArray("offset");
                    offset = new Vector3f(
                            vecArray.get(0).getAsFloat(),
                            vecArray.get(1).getAsFloat(),
                            vecArray.get(2).getAsFloat()
                    );
                }

                return new AnimatableTicTool3DModelData.UnbakedPart(id, model, renderType, toolPart, origin, offset);
            }).toList();

            var boneJson = Optional.ofNullable(jsonObject.has("bone") ? null : jsonObject.getAsJsonObject("bone"));
            var skeleton = boneJson.map(o -> {
                var result = new AnimatableTicTool3DModelData.UnbakedBone("root", List.of(), new ArrayList<>());
                Queue<Pair<AnimatableTicTool3DModelData.UnbakedBone, JsonObject>> queue = new LinkedList<>();
                queue.add(Pair.of(result, o));
                while (!queue.isEmpty()) {
                    var p = queue.remove();
                    var b = p.first;
                    var jo = p.second;
                    jo.entrySet().stream().map(entry -> {
                        if (entry.getKey().startsWith("_") || !entry.getValue().isJsonObject()) return null;
                        return Pair.of(entry.getKey(), (JsonObject) entry.getValue());
                    }).filter(Objects::nonNull).forEach(entry -> {
                        var name = entry.first;
                        var innerJo = entry.second;

                        var boneParts = (innerJo.has("_parts") ?
                                innerJo.getAsJsonArray("_parts").asList() :
                                List.<JsonElement>of()
                        ).stream().map(JsonElement::getAsString).toList();

                        var innerBone = new AnimatableTicTool3DModelData.UnbakedBone(name, boneParts, new ArrayList<>());
                        b.bones().add(innerBone);
                        queue.add(Pair.of(innerBone, innerJo));
                    });
                }
                return result;
            }).orElse(new AnimatableTicTool3DModelData.UnbakedBone(
                    "root", parts.stream().map(AnimatableTicTool3DModelData.UnbakedPart::id).toList(), new ArrayList<>()
            ));

            var transforms = ItemTransforms.NO_TRANSFORMS;
            if (jsonObject.has("display")) {
                transforms = deserializationContext.deserialize(jsonObject.get("display"), ItemTransforms.class);
            }

            var largeTex = jsonObject.has("large_tex") && jsonObject.get("large_tex").getAsBoolean();

            return new AnimatableTicTool3DUnbakedModel(parts, skeleton, transforms, largeTex);
        } catch (IllegalStateException | JsonSyntaxException | ClassCastException | NullPointerException e) {
            throw new JsonParseException("AnimTicTool3DModel Loader found invalid model data.", e);
        }
    }


    @Mod.EventBusSubscriber(modid = TooManyTinkers.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class Registerer {

        @SubscribeEvent
        public static void onRegisterGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
            event.register("anim_tool3d", new AnimatableTicTool3DModelLoader());
        }
    }
}
