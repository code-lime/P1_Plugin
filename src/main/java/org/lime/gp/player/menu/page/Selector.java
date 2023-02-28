package org.lime.gp.player.menu.page;

import com.google.gson.JsonObject;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.lime.Position;
import org.lime.gp.chat.Apply;
import org.lime.gp.database.Rows;
import org.lime.gp.lime;
import org.lime.gp.player.menu.ActionSlot;
import org.lime.gp.player.selector.*;
import org.lime.system;

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

    @Override protected void showGenerate(Rows.UserRow row, Player player, int page, Apply apply) {
        if (player == null) return;
        switch (selector) {
            case Main:
                List<Material> filter = apply.get("filter")
                        .map(v -> Arrays.stream(v.split(","))
                                .map(_v -> system.tryParse(Material.class, _v).or(() -> {
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
                            .add("pos_x", pos.x + "")
                            .add("pos_y", pos.y + "")
                            .add("pos_z", pos.z + "")
                            .add("pos_face_x", posFace.x + "")
                            .add("pos_face_y", posFace.y + "")
                            .add("pos_face_z", posFace.z + "")
                            .add("face", face.name());
                    output.forEach(i -> i.invoke(player, apply, true));
                }).select(player);
                break;
            case ZoneReadonly:
                new ZoneReadonly(
                        Position.of(lime.MainWorld, apply.get("pos1").map(system::getVector).orElseGet(() -> new Vector(0,0,0))),
                        Position.of(lime.MainWorld, apply.get("pos2").map(system::getVector).orElseGet(() -> new Vector(0,0,0)))
                ).select(player);
                break;
            case ZoneMain:
                ZoneMainSelector.create((pos1, pos2, posMain, faceMain) -> {
                    apply
                            .add("pos1", pos1.toSave())
                            .add("pos1_x", pos1.x + "")
                            .add("pos1_y", pos1.y + "")
                            .add("pos1_z", pos1.z + "")
                            .add("pos2", pos2.toSave())
                            .add("pos2_x", pos2.x + "")
                            .add("pos2_y", pos2.y + "")
                            .add("pos2_z", pos2.z + "")
                            .add("posMain", posMain.toSave())
                            .add("posMain_x", posMain.x + "")
                            .add("posMain_y", posMain.y + "")
                            .add("posMain_z", posMain.z + "")
                            .add("faceMain", faceMain.name());
                    output.forEach(i -> i.invoke(player, apply, true));
                }).select(player);
                break;
        }
    }
}

































