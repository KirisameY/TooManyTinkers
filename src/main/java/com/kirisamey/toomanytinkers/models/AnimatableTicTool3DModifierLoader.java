package com.kirisamey.toomanytinkers.models;

import com.google.gson.*;
import com.ibm.icu.impl.Pair;
import com.kirisamey.toomanytinkers.TmtRegistries;
import com.kirisamey.toomanytinkers.TooManyTinkers;
import com.mojang.logging.LogUtils;
import io.vavr.collection.HashMap;
import io.vavr.collection.Stream;
import io.vavr.collection.Vector;
import io.vavr.control.Option;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;
import slimeknights.tconstruct.library.modifiers.ModifierId;

import java.util.Objects;

public class AnimatableTicTool3DModifierLoader implements IGeometryLoader<AnimatableTicTool3DUnbakedModifier> {

    @Override
    public AnimatableTicTool3DUnbakedModifier read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) throws JsonParseException {
        try {
            var id = Option.of(jsonObject.get("id"))
                    .flatMap(s -> Option.of(ModifierId.tryParse(s.getAsString())))
                    .getOrElseThrow(() -> new IllegalArgumentException("inexistent or invalid modifier id found"));

            var level = Option.of(jsonObject.get("level")).map(JsonElement::getAsInt).getOrElse(0);

            var partsJson = jsonObject.getAsJsonObject("parts");
            var parts = AnimatableTicTool3DModelLoader.getUnbakedParts(partsJson);

            var controller = Option.of(jsonObject.get("controller"))
                    .flatMap((location) -> Option.of(ResourceLocation.parse(location.getAsString())))
                    .map(c -> Objects.requireNonNull(TmtRegistries.BONE_CONTROLLERS.get().getValue(c)));

            var transforms = Option.of(jsonObject.get("display"))
                    .map(o -> deserializationContext.<ItemTransforms>deserialize(o, ItemTransforms.class))
                    .getOrElse(ItemTransforms.NO_TRANSFORMS);

            var mods = Option.of(jsonObject.getAsJsonObject("bones")).map(m -> {
                return Stream.ofAll(m.entrySet()).map(entry -> {
                    var boneId = entry.getKey();
                    var obj = entry.getValue().getAsJsonObject();
                    //noinspection DuplicatedCode
                    var newParts = Option.of(obj.getAsJsonArray("new_parts")).map(Stream::ofAll).map(pts -> pts.map(part -> {
                        if (part.isJsonArray()) {
                            var a = part.getAsJsonArray();
                            var pn = a.get(0).getAsString();
                            var of = a.get(1).getAsJsonArray();
                            var off = new Vector3f(
                                    of.get(0).getAsFloat() / 16f,
                                    of.get(1).getAsFloat() / 16f,
                                    of.get(2).getAsFloat() / 16f
                            );
                            return Pair.of(pn, off);
                        }
                        return Pair.of(part.getAsString(), new Vector3f());
                    })).map(Stream::toVector).getOrElse(Vector.empty());
                    var removeParts = Option.of(obj.getAsJsonArray("remove_parts"))
                            .map(pts -> Stream.ofAll(pts).map(JsonElement::getAsString))
                            .map(Stream::toVector)
                            .getOrElse(Vector.empty());

                    return new AnimatableTicTool3DModelData.UnbakedBoneModifier(boneId, newParts, removeParts);
                });
            }).map(Stream::toVector).getOrElse(Vector.empty());

            var marks = Option.of(jsonObject.getAsJsonObject("marks"))
                    .map(AnimatableTicTool3DModelLoader::getMarks)
                    .getOrElse(HashMap.empty());

            return new AnimatableTicTool3DUnbakedModifier(id, level, parts, controller, transforms, mods, marks);
        } catch (IllegalStateException | JsonSyntaxException | ClassCastException | NullPointerException |
                 IndexOutOfBoundsException e) {
            throw new JsonParseException("AnimTicTool3DModifier Loader found invalid model data.", e);
        }
    }

    @Mod.EventBusSubscriber(modid = TooManyTinkers.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ATT3ModifierLoaderRegisterer {

        // 佛具有big，如果两个类实现了同样的泛型接口然后有同名内部类用来注册同一个事件的话，会有一个被忽略然后另一个注册两次，我不得不起个名字区别开

        @SubscribeEvent
        public static void onRegisterGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
            event.register("anim_tool3d_modifier", new AnimatableTicTool3DModifierLoader());
        }
    }
}
