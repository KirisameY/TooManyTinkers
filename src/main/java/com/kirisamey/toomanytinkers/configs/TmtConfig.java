package com.kirisamey.toomanytinkers.configs;

import com.ibm.icu.impl.Pair;
import com.kirisamey.toomanytinkers.TooManyTinkers;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Mod.EventBusSubscriber(modid = TooManyTinkers.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class TmtConfig {

    // <editor-fold desc="Define">
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> EXCLUDE_PAIRS = BUILDER
            .comment("List of <Part, Material> Pair to for exclusion, split by a '%'")
            .comment("'*' & 'modid:*' & 'modid:any/path/*' are supported as wildcards")
            .comment("But '*%*' is not supported, I mean, why the hell would someone want use this?")
            .defineListAllowEmpty("exclude_pairs", List.of(
                    "tconstruct:item/tool/armor/slime/helmet_skull%*"
            ), TmtConfig::validateExclude);

    static public final ForgeConfigSpec SPEC = BUILDER.build();


    private static boolean validateExclude(final Object obj) {
        if (!(obj instanceof final String str)) return false;
        var parts = str.split("%");
        return parts.length == 2
                && (parts[0].split(":").length == 2 || parts[0].equals("*"))
                && (parts[1].split(":").length == 2 || parts[1].equals("*"));
    }

    // </editor-fold>


    // <editor-fold desc="Props">

    private static void parseExcludes() {
        var logger = LogUtils.getLogger();

        var parts = new TmtExcludes.LocationRule(new ArrayList<>(), new ArrayList<>());
        var materials = new TmtExcludes.LocationRule(new ArrayList<>(), new ArrayList<>());
        var byPartPrefixes = new HashMap<ResourceLocation, TmtExcludes.LocationRule>();
        var byPartExacts = new HashMap<ResourceLocation, TmtExcludes.LocationRule>();

        for (var pair : EXCLUDE_PAIRS.get()) {
            // Now I'm really missing pattern matching of C#, FUCK JVAV
            var pairSpl = pair.split("%");
            var part = pairSpl[0];
            var mat = pairSpl[1];
            if (part.equals("*") && mat.equals("*")) {
                logger.warn("zh_cn: 我说不支持'*%*'你二龙马？");
                logger.warn("en_us: Didn't I say '*%*' is fucking not supported?");
            } else if (part.equals("*")) {
                var rlp = checkResourceLocation(mat);
                if (rlp.first) materials.prefixes().add(rlp.second);
                else materials.exacts().add(rlp.second);
                logger.info("Add excluded part: '{}'{}", part, rlp.first ? " as prefix" : "");
            } else if (mat.equals("*")) {
                var rlp = checkResourceLocation(part);
                if (rlp.first) parts.prefixes().add(rlp.second);
                else parts.exacts().add(rlp.second);
                logger.info("Add excluded material: '{}'{}", mat, rlp.first ? " as prefix" : "");
            } else {
                var rlpPart = checkResourceLocation(part);
                var rlpMat = checkResourceLocation(mat);
                var map = rlpPart.first ? byPartPrefixes : byPartExacts;
                var rule = map.computeIfAbsent(rlpPart.second,
                        l -> new TmtExcludes.LocationRule(new ArrayList<>(), new ArrayList<>()));
                if (rlpMat.first) rule.prefixes().add(rlpMat.second);
                else rule.exacts().add(rlpMat.second);
                logger.info("Add excluded pair: <'{}'{}, '{}'{}>",
                        part, rlpPart.first ? " as prefix" : "",
                        mat, rlpMat.first ? " as prefix" : "");
            }
        }

        TmtExcludes.setExcludes(parts, materials, byPartPrefixes, byPartExacts);
    }

    private static Pair<Boolean, ResourceLocation> checkResourceLocation(String str) {
        var parts = str.split(":");
        var namespace = parts[0];
        var path = parts[1];

        var isPrefix = false;
        if (path.endsWith("*")) {
            isPrefix = true;
            path = path.substring(0, path.length() - 1);
        }
        return Pair.of(isPrefix, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    // </editor-fold>


    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        parseExcludes();
    }
}
