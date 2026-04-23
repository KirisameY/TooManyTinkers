package com.kirisamey.toomanytinkers.models.animating;

import com.google.gson.*;
import com.ibm.icu.impl.Pair;
import com.kirisamey.toomanytinkers.TooManyTinkers;
import io.vavr.Tuple;
import io.vavr.Tuple3;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import io.vavr.collection.Stream;
import io.vavr.collection.Vector;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;

import javax.swing.text.html.Option;
import java.util.Objects;
import java.util.Optional;

@Log4j2
public class TmtAnimationSetManager extends SimpleJsonResourceReloadListener {
    public TmtAnimationSetManager(Gson gson) {
        super(gson, "tmt_animations");
    }

    public static final TmtAnimationSetManager DATA_MANAGER = new TmtAnimationSetManager(new Gson());

    @Getter private Map<ResourceLocation, TmtAnimationSet> AnimSetMap = HashMap.empty();

    @Override
    protected void apply(
            java.util.@NonNull Map<ResourceLocation, JsonElement> objectMap,
            @NonNull ResourceManager resourceManager,
            @NonNull ProfilerFiller profiler) {
        AnimSetMap = Stream.ofAll(objectMap.entrySet()).map(e0 -> {
            var location = e0.getKey();
            var element = e0.getValue();

            if (!element.isJsonObject()) return null;
            var obj = element.getAsJsonObject();

            try {
                var animObj = obj.getAsJsonObject("animations");
                var animations = Stream.ofAll(animObj.entrySet()).toMap(entry -> {
                    var animId = entry.getKey();
                    var animContent = entry.getValue().getAsJsonObject();

                    var loopJse = animContent.has("loop") ? animContent.get("loop") : null;
                    TmtAnimation.LoopMode loopMode = TmtAnimation.LoopMode.No;
                    if (loopJse != null) {
                        if (loopJse.getAsString().equals("hold_on_last_frame"))
                            loopMode = TmtAnimation.LoopMode.HoldOnLast;
                        else if (loopJse.getAsBoolean())
                            loopMode = TmtAnimation.LoopMode.Loop;
                    }

                    var length = animContent.has("animation_length") ? animContent.get("animation_length").getAsFloat() : 0;

                    var bonesJso = Optional.ofNullable(animContent.getAsJsonObject("bones"));
                    var bones = bonesJso.map(o -> Stream.ofAll(o.entrySet()).toMap(e -> {
                        var boneName = e.getKey();
                        var boneTimeLine = e.getValue().getAsJsonObject();
                        var bonePosTl = getTimeLine(boneTimeLine, "position"); //Optional.ofNullable(boneTimeLine.getAsJsonObject("position"));
                        var boneRotTl = getTimeLine(boneTimeLine, "rotation");//Optional.ofNullable(boneTimeLine.getAsJsonObject("rotation"));
                        var boneSclTl = getTimeLine(boneTimeLine, "scale");//Optional.ofNullable(boneTimeLine.getAsJsonObject("scale"));
                        return Tuple.of(boneName, new TmtAnimationBoneEntry(
                                getVec3fTl(bonePosTl).map(p -> Tuple.of(
                                        p._1, p._2.mul(1 / 16f).mul(-1, 1, 1), p._3.mul(1 / 16f).mul(-1, 1, 1)
                                )),
                                getVec3fTl(boneRotTl).map(p -> Tuple.of(
                                        p._1, p._2.mul((float) Math.PI / 180f), p._3.mul((float) Math.PI / 180f)
                                )),
                                getVec3fTl(boneSclTl)
                        ));
                    })).orElse(HashMap.empty()).put("_root", new TmtAnimationBoneEntry(
                            Vector.of(Tuple.of(0f, new Vector3f(0.5f, 0, 0.5f), new Vector3f(0.5f, 0, 0.5f))),
                            Vector.empty(), Vector.empty()
                    ));

                    return Tuple.of(animId, new TmtAnimation(loopMode, length, bones));
                });

                return Tuple.of(location, new TmtAnimationSet(animations));
            } catch (IllegalStateException | JsonSyntaxException | ClassCastException | NullPointerException |
                     IndexOutOfBoundsException e) {
                log.error("Tmt Animation Set Loader found invalid animation data.", e);
            }

            return null;
        }).filter(Objects::nonNull).toMap(t -> t);
    }

    private static Optional<JsonObject> getTimeLine(JsonObject in, String name) {
        var e = in.get(name);
        if (e == null) return Optional.empty();
        if (e.isJsonArray()) {
            var result = new JsonObject();
            result.add("0.0", e);
            return Optional.of(result);
        }
        return Optional.of(e.getAsJsonObject());
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static Vector<Tuple3<Float, Vector3f, Vector3f>> getVec3fTl(Optional<JsonObject> vec3Tl) {
        return vec3Tl.map(pTl -> Vector.ofAll(pTl.entrySet().stream()).map(e0 -> {
            var time = Float.valueOf(e0.getKey());
            if (e0.getValue().isJsonArray()) {
                var vec = e0.getValue().getAsJsonArray();
                var vec3 = getVec3(vec);
                return Tuple.of(time, vec3, new Vector3f(vec3));
            } else {
                var obj = e0.getValue().getAsJsonObject();
                var prev = obj.getAsJsonArray("pre");
                var after = obj.getAsJsonArray("post");
                return Tuple.of(time, getVec3(prev), getVec3(after));
            }
        })).orElse(Vector.empty());
    }

    private static Vector3f getVec3(JsonArray vec) {
        return new Vector3f(vec.get(0).getAsFloat(), vec.get(1).getAsFloat(), vec.get(2).getAsFloat());
    }


    @Mod.EventBusSubscriber(modid = TooManyTinkers.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class Registerer {
        @SubscribeEvent
        public static void onAddReloadListener(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(DATA_MANAGER);
        }
    }
}
