package gg.oreo;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class OreoScreen extends Screen {
    private final List<String> categories = List.of("Overview","Combat","Render","World","Player","Network","Staff");
    private final List<Module> modules = List.of(
        new Module("StorageFinder","Chests, barrels, shulkers & containers","Render"),
        new Module("SpawnerFinder","Monster & trial spawners","Render"),
        new Module("EntityESP","Staff visual entity diagnostics","Render"),
        new Module("StaffCamera","Client-side detached camera for authorized testing","Staff"),
        new Module("PacketMonitor","Client network diagnostics","Network"),
        new Module("AntiCheatLogger","Record diagnostic events for staff","Network"),
        new Module("CPSMonitor","Click/timing monitor","Combat"),
        new Module("PositionMonitor","Movement and position diagnostics","Player")
    );

    private static final Map<String, Boolean> states = new HashMap<>();

    public static boolean isModuleEnabled(String name) {
        return states.getOrDefault(name, false);
    }
    private int selected = 0;
    private EditBox search;

    public OreoScreen() { super(Component.literal("Oreo.gg")); }

    @Override
    protected void init() {
        search = new EditBox(font,175,42,Math.max(160,width-355),28,Component.literal("Search modules..."));
        search.setHint(Component.literal("Search modules..."));
        addRenderableWidget(search);

        for (int i=0;i<categories.size();i++) {
            final int index=i;
            addRenderableWidget(Button.builder(Component.literal(categories.get(i)),b->{selected=index;rebuild();})
                .bounds(25,90+i*34,125,28).build());
        }

        addRenderableWidget(Button.builder(Component.literal("☆ Favorites"),b->{})
            .bounds(width-150,42,125,28).build());

        addModules();
    }

    private void rebuild() {
        clearWidgets();
        init();
    }

    private void addModules() {
        int y=90;
        String q=search==null?"":search.getValue().toLowerCase();

        for (Module m:modules) {
            if (!matchesCategory(m) ||
                (!q.isEmpty()&&!m.name.toLowerCase().contains(q)&&!m.description.toLowerCase().contains(q))) continue;

            boolean on = states.getOrDefault(m.name,false);
            String label = m.name + "    " + (on ? "ON" : "OFF");

            addRenderableWidget(Button.builder(Component.literal(label), b -> {
                boolean next = !states.getOrDefault(m.name,false);
                states.put(m.name,next);

                if (m.name.equals("StaffCamera")) {
                    if (next) Freecam.enable(minecraft);
                    else Freecam.disable(minecraft);
                }

                rebuild();
            }).bounds(175,y,Math.max(250,width-240),42).build());

            addRenderableWidget(Button.builder(Component.literal(states.getOrDefault(m.name,false) ? "★" : "☆"),b->{})
                .bounds(width-58,y+7,32,28).build());

            y+=50;
            if(y>height-55) break;
        }
    }

    private boolean matchesCategory(Module m) {
        return selected==0 || categories.get(selected).equals(m.category);
    }

    @Override
    public void render(GuiGraphics g,int mouseX,int mouseY,float delta) {
        int w=width,h=height;
        g.fill(0,0,w,h,0xEA090B10);
        g.fill(16,16,w-16,h-16,0xF2151821);
        g.fill(16,16,155,h-16,0xFF0D1017);
        g.drawString(font,"O",31,27,0xFF6EA8FF,false);
        g.drawString(font,"OREO.GG",50,27,0xFFFFFFFF,false);
        g.drawString(font,"STAFF TOOLS",50,43,0xFF8993A3,false);
        g.drawString(font,categories.get(selected),175,25,0xFFFFFFFF,false);
        g.drawString(font,"DONUTSMP • 1.21.11",w-185,25,0xFF8993A3,false);

        for(int i=0;i<categories.size();i++)
            if(i==selected) g.fill(22,86+i*34,153,120+i*34,0xFF29456F);

        super.render(g,mouseX,mouseY,delta);
    }

    @Override
    public boolean isPauseScreen(){return false;}

    private record Module(String name,String description,String category){}
}
