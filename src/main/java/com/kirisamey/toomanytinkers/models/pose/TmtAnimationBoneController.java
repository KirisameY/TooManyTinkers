package com.kirisamey.toomanytinkers.models.pose;

import com.kirisamey.toomanytinkers.TmtRegistries;
import com.kirisamey.toomanytinkers.models.AnimatableTicTool3DFinalBakedModel;
import com.kirisamey.toomanytinkers.models.AnimatableTicTool3DModelData;
import com.kirisamey.toomanytinkers.models.animating.TmtAnimationSet;
import com.kirisamey.toomanytinkers.models.animating.TmtAnimationSetManager;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import io.vavr.collection.HashMap;
import io.vavr.collection.Vector;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Stack;

@RequiredArgsConstructor
public class TmtAnimationBoneController implements IAnimatableTicTool3DBoneController {

    public TmtAnimationBoneController(ResourceLocation animSetId, ResourceLocation controllerId) {
        this(
                animSetId,
                Objects.requireNonNull(TmtRegistries.ANIM_CONTROLLERS.get().getValue(controllerId))
        );
    }

    @Getter private final ResourceLocation animationSetId;
    @Getter private TmtAnimationSet animationSet;
    @Getter private final ITmtAnimationController controller;

    @Override
    public AnimatableTicTool3DModelData.PosedBone pose(ItemStack itemStack, AnimatableTicTool3DFinalBakedModel model,
                                                       @NotNull ItemDisplayContext itemDisplayContext, Matrix4f outTransform) {
        if (animationSet == null) {
            animationSet = TmtAnimationSetManager.DATA_MANAGER.getAnimSetMap().getOrElse(animationSetId, new TmtAnimationSet(HashMap.empty()));
        }

        //noinspection DuplicatedCode
        Stack<Tuple2<
                ArrayList<AnimatableTicTool3DModelData.PosedBone>,
                AnimatableTicTool3DModelData.BakedBone
                >> pushStack = new Stack<>();
        Stack<Tuple3<
                ArrayList<AnimatableTicTool3DModelData.PosedBone>,
                AnimatableTicTool3DModelData.BakedBone,
                ArrayList<AnimatableTicTool3DModelData.PosedBone>
                >> poseStack = new Stack<>();

        var finalList = new ArrayList<AnimatableTicTool3DModelData.PosedBone>();
        pushStack.push(Tuple.of(finalList, model.getSkeleton()));

        while (!pushStack.empty()) {
            var t = pushStack.pop();
            var parentList = t._1;
            var bone = t._2;
            var selfList = new ArrayList<AnimatableTicTool3DModelData.PosedBone>();
            poseStack.push(Tuple.of(parentList, bone, selfList));
            bone.bones().forEach(b -> {
                pushStack.push(Tuple.of(selfList, b));
            });
        }

        var animInfo = controller.getPose(itemStack, itemDisplayContext);
        var animId = animInfo._1;
        var animTime = animInfo._2;

        while (!poseStack.empty()) {
            var t = poseStack.pop();
            var parentList = t._1;
            var bone = t._2;
            var selfList = t._3;

            var anim = animationSet.animations().get(animId);
            var transform = anim.flatMap(a -> {
                return a.getBones().get(bone.id()).map(b -> {
                    var tRrS = b.getInterpolatedTRrS(animTime);
                    var tl = tRrS._1;
                    var rr = tRrS._2;
                    var s = tRrS._3;
                    return new Matrix4f().translate(tl)
                            .rotateXYZ(rr)
                            .scale(s);
                });
            }).getOrElse(new Matrix4f());

            if (poseStack.empty()) {
                transform.mulLocal(outTransform);
            }

            var newBone = new AnimatableTicTool3DModelData.PosedBone(bone.id(), bone.parts(), Vector.ofAll(selfList), transform);
            parentList.add(newBone);
        }

        return finalList.get(0);
    }

}
