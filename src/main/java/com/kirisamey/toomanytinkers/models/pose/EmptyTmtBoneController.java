package com.kirisamey.toomanytinkers.models.pose;

import com.kirisamey.toomanytinkers.models.AnimatableTicTool3DModelData;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import io.vavr.collection.Vector;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Stack;

public class EmptyTmtBoneController implements IAnimatableTicTool3DBoneController {

    private EmptyTmtBoneController() {
    }

    public static final EmptyTmtBoneController INSTANCE = new EmptyTmtBoneController();

    @Override
    public AnimatableTicTool3DModelData.PosedBone pose(ItemStack itemStack, AnimatableTicTool3DModelData.BakedBone root, Matrix4f transform) {
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
        pushStack.push(Tuple.of(finalList, root));

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

        while (!poseStack.empty()) {
            var t = poseStack.pop();
            var parentList = t._1;
            var bone = t._2;
            var selfList = t._3;

            var trans = poseStack.empty() ? transform : new Matrix4f();

            var newBone = new AnimatableTicTool3DModelData.PosedBone(bone.id(), bone.parts(), Vector.ofAll(selfList), trans);
            parentList.add(newBone);
        }

        return finalList.get(0);
    }
}
