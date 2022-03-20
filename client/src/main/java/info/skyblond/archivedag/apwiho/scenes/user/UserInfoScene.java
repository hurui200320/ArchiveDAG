package info.skyblond.archivedag.apwiho.scenes.user;

import info.skyblond.archivedag.apwiho.scenes.HomeScene;
import info.skyblond.archivedag.apwiho.scenes.templates.BasicPage;
import info.skyblond.archivedag.apwiho.services.DialogService;
import info.skyblond.archivedag.apwiho.services.GrpcClientService;
import info.skyblond.archivedag.arudaz.protos.common.Empty;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.text.Font;

public class UserInfoScene extends BasicPage {

    public UserInfoScene(Scene currentScene, HomeScene goBackScene) {
        super(currentScene, goBackScene);
    }

    private Label userInfoLabel;

    @Override
    protected String getPagePath() {
        return "Home > User info";
    }

    @Override
    protected Parent generateBorderCenter() {
        this.userInfoLabel = new Label();
        this.userInfoLabel.setFont(Font.font(16));
        return this.userInfoLabel;
    }

    @Override
    protected void refreshBorderCenter() {
        var alert = DialogService.getInstance().showWaitingDialog("Requesting user info...");
        var userInfoStub = GrpcClientService.getInstance().getUserInfoServiceFutureStub();
        var request = userInfoStub.whoAmI(Empty.getDefaultInstance());
        try {
            var result = request.get();
            var stringBuilder = new StringBuilder();
            stringBuilder.append("Username: ").append(result.getUsername()).append("\n\n");
            stringBuilder.append("Roles: ").append("\n");
            result.getRoleList().forEach(r -> stringBuilder.append("  + ").append(r).append("\n"));
            stringBuilder.append("\nOwned group: ").append(result.getOwnedGroupList().size()).append("\n\n");
            stringBuilder.append("Joined group: ").append(result.getJoinedGroupList().size()).append("\n");
            this.userInfoLabel.setText(stringBuilder.toString());
        } catch (Throwable t) {
            DialogService.getInstance().showExceptionDialog(t, "Failed to get user info");
        } finally {
            alert.close();
        }
    }
}
