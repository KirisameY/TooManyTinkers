package com.kirisamey.toomanytinkers.models.animating;

import com.ibm.icu.impl.Pair;
import io.vavr.Tuple;
import io.vavr.Tuple3;
import io.vavr.collection.Vector;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.function.TriFunction;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@SuppressWarnings("ClassCanBeRecord")
@RequiredArgsConstructor
public class TmtAnimationBoneEntry {
    @Getter private final Vector<Pair<Float, Vector3f>> position;
    @Getter private final Vector<Pair<Float, Vector3f>> rotateRad;
    @Getter private final Vector<Pair<Float, Vector3f>> scale;

    public Tuple3<Vector3f, Vector3f, Vector3f> getInterpolatedTRrS(float time) {
        time /= 20f;
        TriFunction<Vector3f, Vector3f, Float, Vector3f> lerp0 = (v1, v2, t) ->
                v1.lerp(v2, t, new Vector3f());
        var pos = interpolateVec3List(position, time, lerp0, false);
        var scl = interpolateVec3List(scale, time, lerp0, true);
        var rtR = interpolateVec3List(rotateRad, time, lerp0, false);
//        var rtR = interpolateVec3List(rotateRad, time, (v1, v2, t) -> {
//            var q1 = new Quaternionf().rotationXYZ(v1.x, v1.y, v1.z);
//            var q2 = new Quaternionf().rotationXYZ(v2.x, v2.y, v2.z);
//            return q1.slerp(q2, t).getEulerAnglesXYZ(new Vector3f());
//        }, false);
        return Tuple.of(pos, rtR, scl);
    }

    private static Vector3f interpolateVec3List(
            Vector<Pair<Float, Vector3f>> list, float time,
            TriFunction<Vector3f, Vector3f, Float, Vector3f> lerp, boolean defaultAs1) {
        if (list.isEmpty()) return new Vector3f(defaultAs1 ? 1 : 0);

        Vector3f last = list.get(0).second;
        float lastTime = 0;
        for (Pair<Float, Vector3f> pair : list) {
            var t = pair.first;
            var v = pair.second;
            if (t < time) {
                last = v;
                lastTime = t;
                continue;
            } else if (t > time && t != lastTime) {
                t = (time - lastTime) / (t - lastTime);
            } else {
                t = 1f;
            }
            return lerp.apply(last, v, t);
        }
        return last;
    }
}
