package org.lime.gp.player.menu.page;

import com.google.gson.JsonObject;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.lime.Position;
import org.lime.gp.chat.Apply;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.lime;
import org.lime.gp.player.menu.ActionSlot;
import org.lime.gp.player.selector.*;
import org.lime.system.utils.EnumUtils;
import org.lime.system.utils.MathUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Selector extends Base {
    public List<ActionSlot> output = new ArrayList<>();
    public SelectorType selector;

    public Selector(JsonObject json) {
        super(json);
        if (json.has("output")) json.get("output").getAsJsonArray().forEach(kv -> output.add(ActionSlot.parse(this, kv.getAsJsonObject())));
        selector = SelectorType.of(json.get("selector").getAsString()).orElseThrow();
    }

    @Override protected void showGenerate(UserRow row, Player player, int page, Apply apply) {
        if (player == null) return;
        switch (selector) {
            case Main:
                List<Material> filter = apply.get("filter")
                        .map(v -> Arrays.stream(v.split(","))
                                .map(_v -> EnumUtils.tryParse(Material.class, _v).or(() -> {
                                    lime.logOP("Filter material '"+_v+"' not founded!");
                                    return Optional.empty();
                                }))
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .collect(Collectors.toList())
                        )
                        .orElse(null);
                MainSelector.create(filter == null ? block -> true : block -> filter.contains(block.getType()), (pos, face) -> {
                    Position posFace = pos.add(face.getModX(), face.getModY(), face.getModZ());
                    apply
                            .add("pos", pos.toSave())
                            .add("pos_x", String.valueOf(pos.x))
                            .add("pos_y", String.valueOf(pos.y))
                            .add("pos_z", String.valueOf(pos.z))
                            .add("pos_face_x", String.valueOf(posFace.x))
                            .add("pos_face_y", String.valueOf(posFace.y))
                            .add("pos_face_z", String.valueOf(posFace.z))
                            .add("face", face.name());
                    output.forEach(i -> i.invoke(player, apply, true));
                }).select(player);
                break;
            case ZoneReadonly:
                new ZoneReadonly(
                        Position.of(lime.MainWorld, apply.get("pos1").map(MathUtils::getVector).orElseGet(() -> new Vector(0,0,0))),
                        Position.of(lime.MainWorld, apply.get("pos2").map(MathUtils::getVector).orElseGet(() -> new Vector(0,0,0)))
                ).select(player);
                break;
            case ZoneMain:
                ZoneMainSelector.create((pos1, pos2, posMain, faceMain) -> {
                    apply
                            .add("pos1", pos1.toSave())
                            .add("pos1_x", String.valueOf(pos1.x))
                            .add("pos1_y", String.valueOf(pos1.y))
                            .add("pos1_z", String.valueOf(pos1.z))
                            .add("pos2", pos2.toSave())
                            .add("pos2_x", String.valueOf(pos2.x))
                            .add("pos2_y", String.valueOf(pos2.y))
                            .add("pos2_z", String.valueOf(pos2.z))
                            .add("posMain", posMain.toSave())
                            .add("posMain_x", String.valueOf(posMain.x))
                            .add("posMain_y", String.valueOf(posMain.y))
                            .add("posMain_z", String.valueOf(posMain.z))
                            .add("faceMain", faceMain.name());
                    output.forEach(i -> i.invoke(player, apply, true));
                }).select(player);
                break;
        }
    }
}

































