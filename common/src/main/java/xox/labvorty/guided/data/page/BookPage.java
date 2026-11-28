package xox.labvorty.guided.data.page;

public interface BookPage {
    void render(PageRenderContext context);
    default void tick() {}
    default void onOpen() {}
    default void onClose() {}
    default boolean mouseClicked(double x,double y,int button){return false;}
    default boolean mouseReleased(double x,double y,int button){return false;}
    default boolean mouseDragged(double x,double y,int button,double dx,double dy){return false;}
    default boolean mouseScrolled(double x,double y,double horizontal,double vertical){return false;}
    default boolean keyPressed(int key,int scan,int modifiers){return false;}
}
