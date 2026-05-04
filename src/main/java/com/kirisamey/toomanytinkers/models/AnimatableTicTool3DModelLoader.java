package com.kirisamey.toomanytinkers.models;

import com.google.gson.*;
import com.ibm.icu.impl.Pair;
import com.kirisamey.toomanytinkers.TmtRegistries;
import com.kirisamey.toomanytinkers.TooManyTinkers;
import com.kirisamey.toomanytinkers.models.pose.TmtAnimationControllers;
import com.kirisamey.toomanytinkers.rendering.TmtRenderTypeGetters;
import com.mojang.logging.LogUtils;
import io.vavr.Tuple;
import io.vavr.Tuple3;
import io.vavr.Tuple4;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import io.vavr.collection.Stream;
import io.vavr.collection.Vector;
import io.vavr.control.Option;
import lombok.extern.log4j.Log4j2;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Stack;

@Log4j2
public class AnimatableTicTool3DModelLoader implements IGeometryLoader<AnimatableTicTool3DUnbakedModel> {

    @Override
    public AnimatableTicTool3DUnbakedModel read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) throws JsonParseException {
        try {
            var partsJson = jsonObject.getAsJsonObject("parts");
            var parts = getUnbakedParts(partsJson);

            var boneJson = Option.of(jsonObject.getAsJsonObject("bone"));

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

                pushStack.push(Tuple.of(finalList, "_root", o));
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

                    //noinspection DuplicatedCode
                    var boneParts = Vector.ofAll(jsonObj.has("_parts") ?
                            Stream.ofAll(jsonObj.getAsJsonArray("_parts")).map(e -> {
                                if (e.isJsonArray()) {
                                    var a = e.getAsJsonArray();
                                    var n = a.get(0).getAsString();
                                    var of = a.get(1).getAsJsonArray();
                                    var off = new Vector3f(
                                            of.get(0).getAsFloat() / 16f,
                                            of.get(1).getAsFloat() / 16f,
                                            of.get(2).getAsFloat() / 16f
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
            }).getOrElse(new AnimatableTicTool3DModelData.UnbakedBone(
                    "_root", parts.map(p -> Pair.of(p.id(), new Vector3f())), Vector.of()
            ));

            var controllerId = jsonObject.has("controller") ?
                    ResourceLocation.parse(jsonObject.get("controller").getAsString()) :
                    TmtAnimationControllers.EMPTY_BONE_CONTROLLER.getId();
            var controller = Objects.requireNonNull(TmtRegistries.BONE_CONTROLLERS.get().getValue(controllerId));

            var transforms = ItemTransforms.NO_TRANSFORMS;
            if (jsonObject.has("display")) {
                transforms = deserializationContext.deserialize(jsonObject.get("display"), ItemTransforms.class);
            }

            var largeTex = jsonObject.has("large_tex") && jsonObject.get("large_tex").getAsBoolean();

            var marks = Option.of(jsonObject.getAsJsonObject("marks"))
                    .map(AnimatableTicTool3DModelLoader::getMarks)
                    .getOrElse(HashMap.empty());

            var mods = Option.of(jsonObject.getAsJsonArray("modifiers")).map(jsa -> {
                return Stream.ofAll(jsa)
                        .map(JsonElement::getAsString)
                        .map(ResourceLocation::parse)
                        .toVector();
            }).getOrElse(Vector.empty());

            return new AnimatableTicTool3DUnbakedModel(parts, skeleton, controller, transforms, largeTex, marks, mods);
        } catch (IllegalStateException | JsonSyntaxException | ClassCastException | NullPointerException |
                 IndexOutOfBoundsException e) {
            throw new JsonParseException("AnimTicTool3DModel Loader found invalid model data.", e);
        }
    }

    static Vector<AnimatableTicTool3DModelData.UnbakedPart> getUnbakedParts(JsonObject partsJson) {
        return Vector.ofAll(partsJson.entrySet().stream().map(e -> {
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
                        vecArray.get(0).getAsFloat() / 16f + 0.5f,
                        vecArray.get(1).getAsFloat() / 16f,
                        vecArray.get(2).getAsFloat() / 16f + 0.5f
                );
            }

            return new AnimatableTicTool3DModelData.UnbakedPart(id, model, renderType, toolPart, origin);
        }));
    }

    static Map<String, Vector3f> getMarks(JsonObject obj) {
        return Stream.ofAll(obj.entrySet()).toMap(entry -> {
            var name = entry.getKey();
            var v = entry.getValue().getAsJsonArray();
            var value = new Vector3f(
                    v.get(0).getAsFloat() / 16f,
                    v.get(1).getAsFloat() / 16f,
                    v.get(2).getAsFloat() / 16f
            );
            return Tuple.of(name, value);
        });
    }


    @Mod.EventBusSubscriber(modid = TooManyTinkers.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ATT3ModelLoaderRegisterer {

        // 佛具有big，如果两个类实现了同样的泛型接口然后有同名内部类用来注册同一个事件的话，会有一个被忽略然后另一个注册两次，我不得不起个名字区别开
        // Fuck Forge

        @SubscribeEvent
        public static void onRegisterGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
            event.register("anim_tool3d", new AnimatableTicTool3DModelLoader());
        }
    }
}
