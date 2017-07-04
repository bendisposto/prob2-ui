import com.google.inject.Guice;
import com.google.inject.Injector;
import de.prob2.ui.MainController;
import de.prob2.ui.internal.ProB2Module;
import de.prob2.ui.project.NewProjectStage;
import javafx.scene.Parent;
import javafx.stage.Stage;
import org.junit.Test;
import org.loadui.testfx.GuiTest;

public class NewProjectTest extends GuiTest{

    boolean mainStage = true;

    @Override
    public Parent getRootNode(){
        if(mainStage) {
            Injector injector = Guice.createInjector(com.google.inject.Stage.PRODUCTION, new ProB2Module());
            return injector.getInstance(MainController.class);
        } else {
            Injector injector = Guice.createInjector(com.google.inject.Stage.PRODUCTION, new ProB2Module());
            final Stage newProjectStage = injector.getInstance(NewProjectStage.class);
            newProjectStage.showAndWait();
            newProjectStage.toFront();
            return newProjectStage.getScene().getRoot();
        }
    }

    @Test
    public void newProjectTest() throws Exception{
        click("#projectTP");
        click("#newProjectButton");
        mainStage = false;
        click("#projectNameField");
        type("Test");
        click("#finishButton");
        mainStage = true;
        //Add machine
    }
}