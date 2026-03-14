package com.kirisamey.toomanytinkers.models;

import com.google.gson.*;
import com.ibm.icu.impl.Pair;
import com.kirisamey.toomanytinkers.TmtRegistries;
import com.kirisamey.toomanytinkers.TooManyTinkers;
import com.kirisamey.toomanytinkers.rendering.TmtRenderTypeGetters;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import io.vavr.Tuple4;
import io.vavr.collection.Vector;
import lombok.extern.log4j.Log4j2;
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
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Log4j2
public class AnimatableTicTool3DModelLoader implements IGeometryLoader<AnimatableTicTool3DUnbakedModel> {
    @Override
    public AnimatableTicTool3DUnbakedModel read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) throws JsonParseException {
        try {
            var partsJson = jsonObject.getAsJsonObject("parts");
            var parts = Vector.ofAll(partsJson.entrySet().stream().map(e -> {
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

                return new AnimatableTicTool3DModelData.UnbakedPart(id, model, renderType, toolPart, origin);
            }));


            var boneJson = Optional.ofNullable(jsonObject.has("bone") ? null : jsonObject.getAsJsonObject("bone"));

            var skeleton = boneJson.map(o -> {
                Stack<Tuple3<
                        ArrayList<AnimatableTicTool3DModelData.UnbakedBone>,
                        String, JsonObject
                        >> pushStack = new Stack<>();
                Stack<Tuple4<
                        ArrayList<AnimatableTicTool3DModelData.UnbakedBone>,
                        String, JsonObject,
                        ArrayList<AnimatableTicTool3DModelData.UnbakedBone>
                        >> bakeStack = new Stack<>();

                ArrayList<AnimatableTicTool3DModelData.UnbakedBone> finalList = new ArrayList<>();

                pushStack.push(Tuple.of(finalList, "root", o));
                while (!pushStack.empty()) {
                    var tuple = pushStack.pop();
                    var parentList = tuple._1;
                    var name = tuple._2;
                    var jsonObj = tuple._3;
                    var selfList = new ArrayList<AnimatableTicTool3DModelData.UnbakedBone>();
                    bakeStack.push(Tuple.of(parentList, name, jsonObj, selfList));

                    jsonObj.entrySet().stream().map(entry -> {
                        if (entry.getKey().startsWith("_") || !entry.getValue().isJsonObject()) return null;
                        return Pair.of(entry.getKey(), (JsonObject) entry.getValue());
                    }).filter(Objects::nonNull).forEach(pair -> {
                        pushStack.push(Tuple.of(selfList, pair.first, pair.second));
                    });
                }

                while (!bakeStack.empty()) {
                    var tuple = bakeStack.pop();
                    var parentList = tuple._1;
                    var name = tuple._2;
                    var jsonObj = tuple._3;
                    var selfList = tuple._4;

                    var boneParts = Vector.ofAll(jsonObj.has("_parts") ?
                            jsonObj.getAsJsonArray("_parts").asList().stream().map(e -> {
                                if (e.isJsonArray()) {
                                    var a = e.getAsJsonArray();
                                    var n = a.get(0).getAsString();
                                    var of = a.get(1).getAsJsonArray();
                                    var off = new Vector3f(
                                            of.get(0).getAsFloat(),
                                            of.get(1).getAsFloat(),
                                            of.get(2).getAsFloat()
                                    );
                                    return Pair.of(n, off);
                                }
                                return Pair.of(e.getAsString(), new Vector3f());
                            }) :
                            Stream.<Pair<String, Vector3f>>of()
                    );
                    var newBone = new AnimatableTicTool3DModelData.UnbakedBone(name, boneParts, Vector.ofAll(selfList));

                    parentList.add(newBone);
                }

                return finalList.get(0);
            }).orElse(new AnimatableTicTool3DModelData.UnbakedBone(
                    "root", parts.map(p -> Pair.of(p.id(), new Vector3f())), Vector.of()
            ));

            var transforms = ItemTransforms.NO_TRANSFORMS;
            if (jsonObject.has("display")) {
                transforms = deserializationContext.deserialize(jsonObject.get("display"), ItemTransforms.class);
            }

            var largeTex = jsonObject.has("large_tex") && jsonObject.get("large_tex").getAsBoolean();

            return new AnimatableTicTool3DUnbakedModel(parts, skeleton, transforms, largeTex);
        } catch (IllegalStateException | JsonSyntaxException | ClassCastException | NullPointerException |
                 IndexOutOfBoundsException e) {
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
