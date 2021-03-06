/*
 * Copyright 2019 Patrik Karlström.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mapton.core.ui;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.prefs.PreferenceChangeEvent;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.apache.commons.lang3.SystemUtils;
import org.controlsfx.control.Notifications;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.action.Action;
import org.controlsfx.control.action.ActionGroup;
import org.controlsfx.control.action.ActionUtils;
import org.mapton.api.MDocumentInfo;
import org.mapton.api.MEngine;
import org.mapton.api.MKey;
import org.mapton.api.MOptions;
import org.mapton.api.Mapton;
import static org.mapton.api.Mapton.getIconSizeContextMenu;
import static org.mapton.api.Mapton.getIconSizeToolBar;
import org.mapton.core.Initializer;
import org.mapton.core.ui.bookmark.BookmarkView;
import org.openide.awt.Actions;
import se.trixon.almond.nbp.Almond;
import se.trixon.almond.nbp.AlmondOptions;
import se.trixon.almond.nbp.dialogs.NbAboutFx;
import se.trixon.almond.util.AboutModel;
import se.trixon.almond.util.Dict;
import se.trixon.almond.util.GlobalStateChangeEvent;
import se.trixon.almond.util.SystemHelper;
import se.trixon.almond.util.fx.FxActionSwing;
import se.trixon.almond.util.fx.FxActionSwingCheck;
import se.trixon.almond.util.fx.FxHelper;
import se.trixon.almond.util.icons.material.MaterialIcon;

/**
 *
 * @author Patrik Karlström
 */
public class AppToolBar extends ToolBar {

    private static final boolean IS_MAC = SystemUtils.IS_OS_MAC;
    private final AlmondOptions mAlmondOptions = AlmondOptions.INSTANCE;
    private Action mAttributionAction;
    private PopOver mAttributionPopOver;
    private AttributionView mAttributionView;
    private Action mBookmarkAction;
    private PopOver mBookmarkPopOver;
    private FxActionSwing mHomeAction;
    private Action mLayerAction;
    private PopOver mLayerPopOver;
    private final MOptions mOptions = MOptions.getInstance();
    private final HashSet<PopOver> mPopOvers = new HashSet<>();
    private final HashMap<PopOver, Long> mPopoverClosingTimes = new HashMap<>();
    private FxActionSwing mRulerAction;
    private SearchView mSearchView;
    private Action mStyleAction;
    private PopOver mStylePopOver;
    private FxActionSwing mSysAboutAction;
    private Action mSysHelpAction;
    private FxActionSwing mSysOptionsAction;
    private FxActionSwing mSysPluginsAction;
    private FxActionSwing mSysQuitAction;
    private FxActionSwingCheck mSysViewAlwaysOnTopAction;
    private FxActionSwingCheck mSysViewFullscreenAction;
    private FxActionSwingCheck mSysViewMapAction;
    private FxActionSwing mSysViewResetAction;
    private Action mToolboxAction;
    private PopOver mToolboxPopOver;

    public AppToolBar() {
        initPopOvers();
        initActionsFx();
        initActionsSwing();
        init();
        initListeners();
    }

    public void activateSearch() {
        Platform.runLater(() -> {
            getScene().getWindow().requestFocus();
            mSearchView.getPresenter().requestFocus();
            ((TextField) mSearchView.getPresenter()).clear();
        });
    }

    public void displayMenu() {
        Platform.runLater(() -> {
            Node node = getItems().get(getItems().size() - 1);
            if (node instanceof MenuButton) {
                ((MenuButton) node).show();
            }
        });
    }

    public void toogleAttributionPopOver() {
        tooglePopOver(mAttributionPopOver, mAttributionAction);
    }

    public void toogleBookmarkPopOver() {
        tooglePopOver(mBookmarkPopOver, mBookmarkAction);
    }

    public void toogleLayerPopOver() {
        tooglePopOver(mLayerPopOver, mLayerAction);
    }

    public void toogleStylePopOver() {
        tooglePopOver(mStylePopOver, mStyleAction);
    }

    public void toogleToolboxPopOver() {
        tooglePopOver(mToolboxPopOver, mToolboxAction);
    }

    void refreshEngine(MEngine engine) {
        mStyleAction.setDisabled(engine.getStyleView() == null);
    }

    private void init() {
        setStyle("-fx-spacing: 0px;");
        setPadding(Insets.EMPTY);
        ActionGroup viewActionGroup = new ActionGroup(Dict.VIEW.toString(),
                mSysViewAlwaysOnTopAction,
                ActionUtils.ACTION_SEPARATOR,
                mSysViewResetAction
        );

        if (!IS_MAC) {
            viewActionGroup.getActions().add(0, mSysViewFullscreenAction);
        }

        ActionGroup systemActionGroup;
        if (IS_MAC) {
            systemActionGroup = new ActionGroup(Dict.MENU.toString(), MaterialIcon._Navigation.MENU.getImageView(getIconSizeToolBar()),
                    viewActionGroup,
                    ActionUtils.ACTION_SEPARATOR,
                    mSysPluginsAction,
                    ActionUtils.ACTION_SEPARATOR,
                    mSysHelpAction
            );
        } else {
            systemActionGroup = new ActionGroup(Dict.MENU.toString(), MaterialIcon._Navigation.MENU.getImageView(getIconSizeToolBar()),
                    viewActionGroup,
                    ActionUtils.ACTION_SEPARATOR,
                    mSysOptionsAction,
                    mSysPluginsAction,
                    ActionUtils.ACTION_SEPARATOR,
                    mSysHelpAction,
                    mSysAboutAction,
                    ActionUtils.ACTION_SEPARATOR,
                    mSysQuitAction
            );
        }

        ArrayList<Action> actions = new ArrayList<>();
        actions.addAll(Arrays.asList(
                mHomeAction,
                mToolboxAction,
                mRulerAction,
                mLayerAction,
                mBookmarkAction,
                mStyleAction,
                ActionUtils.ACTION_SPAN,
                mAttributionAction,
                mSysViewMapAction,
                systemActionGroup
        ));

        Platform.runLater(() -> {
            ActionUtils.updateToolBar(this, actions, ActionUtils.ActionTextBehavior.HIDE);

            Button styleButton = (Button) getItems().get(5);
            Double w = styleButton.prefWidthProperty().getValue();
            FxHelper.adjustButtonWidth(getItems().stream(), getIconSizeContextMenu() * 1.5);
            styleButton.setPrefWidth(w);
            styleButton.textProperty().bind(mStyleAction.textProperty());

            getItems().stream().filter((item) -> (item instanceof ButtonBase))
                    .map((item) -> (ButtonBase) item).forEachOrdered((buttonBase) -> {
                FxHelper.undecorateButton(buttonBase);
            });

            mSearchView = new SearchView();
            getItems().add(5, mSearchView.getPresenter());
        });
    }

    private void initActionsFx() {
        //Bookmark
        mBookmarkAction = new Action(Dict.BOOKMARKS.toString(), (ActionEvent event) -> {
            if (usePopOver()) {
                if (shouldOpen(mLayerPopOver)) {
                    mBookmarkPopOver.show((Node) event.getSource());
                }
            } else {
                SwingUtilities.invokeLater(() -> {
                    Actions.forID("Mapton", "org.mapton.core.actions.BookmarkAction").actionPerformed(null);
                });
            }
        });
        mBookmarkAction.setGraphic(MaterialIcon._Action.BOOKMARK_BORDER.getImageView(getIconSizeToolBar()));
        mBookmarkAction.setSelected(mOptions.isBookmarkVisible());

        //Layer
        mLayerAction = new Action(Dict.LAYERS.toString(), (ActionEvent event) -> {
            if (usePopOver()) {
                if (shouldOpen(mLayerPopOver)) {
                    mLayerPopOver.show((Node) event.getSource());
                }
            } else {
                SwingUtilities.invokeLater(() -> {
                    Actions.forID("Mapton", "org.mapton.core.actions.LayerAction").actionPerformed(null);
                });
            }
        });
        mLayerAction.setGraphic(MaterialIcon._Maps.LAYERS.getImageView(getIconSizeToolBar()));
        mLayerAction.setSelected(mOptions.isBookmarkVisible());

        //mToolbox
        mToolboxAction = new Action(Dict.TOOLBOX.toString(), (event) -> {
            if (usePopOver()) {
                if (shouldOpen(mToolboxPopOver)) {
                    mToolboxPopOver.show((Node) event.getSource());
                }
            } else {
                SwingUtilities.invokeLater(() -> {
                    Actions.forID("Mapton", "org.mapton.core.actions.ToolboxAction").actionPerformed(null);
                });
            }
        });
        mToolboxAction.setGraphic(MaterialIcon._Places.BUSINESS_CENTER.getImageView(getIconSizeToolBar()));

        //Style
        mStyleAction = new Action(String.format("%s & %s", Dict.TYPE.toString(), Dict.STYLE.toString()), (ActionEvent event) -> {
            if (shouldOpen(mStylePopOver)) {
                BorderPane pane = (BorderPane) mStylePopOver.getContentNode();
                pane.setCenter(Mapton.getEngine().getStyleView());
                mStylePopOver.show((Node) event.getSource());
            }
        });
        mStyleAction.setGraphic(MaterialIcon._Image.COLOR_LENS.getImageView(getIconSizeToolBar()));
        mStyleAction.setDisabled(true);

        //Copyright
        mAttributionAction = new Action("Copyright", (ActionEvent event) -> {
            if (shouldOpen(mAttributionPopOver)) {
                mAttributionPopOver.show((Node) event.getSource());
            }
        });
        mAttributionAction.setGraphic(MaterialIcon._Action.COPYRIGHT.getImageView(getIconSizeToolBar()));
        mAttributionAction.setDisabled(true);

        //Help
        mSysHelpAction = new Action(Dict.HELP.toString(), (ActionEvent event) -> {
            SystemHelper.desktopBrowse("https://mapton.org/help/");
        });
        mSysHelpAction.setAccelerator(KeyCombination.keyCombination("F1"));
    }

    private void initActionsSwing() {
        //Home
        mHomeAction = new FxActionSwing(Dict.HOME.toString(), () -> {
            Actions.forID("Mapton", "org.mapton.core.actions.HomeAction").actionPerformed(null);
        });
        mHomeAction.setGraphic(MaterialIcon._Action.HOME.getImageView(getIconSizeToolBar()));

        //Ruler
        mRulerAction = new FxActionSwing(Dict.MEASURE.toString(), () -> {
            Actions.forID("Mapton", "org.mapton.core.actions.RulerAction").actionPerformed(null);
        });
        mRulerAction.setGraphic(MaterialIcon._Editor.SPACE_BAR.getImageView(getIconSizeToolBar()));
//
//
//
        //Full screen
        mSysViewFullscreenAction = new FxActionSwingCheck(Dict.FULL_SCREEN.toString(), () -> {
            if (IS_MAC) {
                Actions.forID("Almond", "se.trixon.almond.nbp.osx.actions.ToggleFullScreenAction").actionPerformed(null);
            } else {
                Actions.forID("Window", "org.netbeans.core.windows.actions.ToggleFullScreenAction").actionPerformed(null);
            }
        });
        mSysViewFullscreenAction.setAccelerator(KeyCombination.keyCombination("F11"));

        //Map
        mSysViewMapAction = new FxActionSwingCheck(Dict.MAP.toString(), () -> {
            Actions.forID("Mapton", "org.mapton.core.actions.OnlyMapAction").actionPerformed(null);
        });
        mSysViewMapAction.setGraphic(MaterialIcon._Maps.MAP.getImageView(getIconSizeToolBar()));
        mSysViewMapAction.setAccelerator(KeyCombination.keyCombination("F12"));
        mSysViewMapAction.setSelected(mOptions.isMapOnly());

        //OnTop
        mSysViewAlwaysOnTopAction = new FxActionSwingCheck(Dict.ALWAYS_ON_TOP.toString(), () -> {
            Actions.forID("View", "se.trixon.almond.nbp.StayOnTopAction").actionPerformed(null);
        });
        mSysViewAlwaysOnTopAction.setSelected(mAlmondOptions.getAlwaysOnTop());

        //Reset
        mSysViewResetAction = new FxActionSwing(Dict.RESET_WINDOWS.toString(), () -> {
            Actions.forID("Window", "org.netbeans.core.windows.actions.ResetWindowsAction").actionPerformed(null);
        });
//
//
//
        //Plugins
        mSysPluginsAction = new FxActionSwing(Dict.PLUGINS.toString(), () -> {
            final java.awt.event.ActionEvent dummySwingActionEvent = new java.awt.event.ActionEvent(new JButton(), 0, "");
            Actions.forID("System", "org.netbeans.modules.autoupdate.ui.actions.PluginManagerAction").actionPerformed(dummySwingActionEvent);
        });

        //options
        mSysOptionsAction = new FxActionSwing(Dict.OPTIONS.toString(), () -> {
            Actions.forID("Mapton", "org.mapton.core.actions.OptionsAction").actionPerformed(null);
        });
        if (!IS_MAC) {
            mSysOptionsAction.setAccelerator(new KeyCodeCombination(KeyCode.COMMA, KeyCombination.SHORTCUT_DOWN));
        }

        //About
        mSysAboutAction = new FxActionSwing(String.format(Dict.ABOUT_S.toString(), "Mapton"), () -> {
            AboutModel aboutModel = new AboutModel(SystemHelper.getBundle(Initializer.class, "about"), SystemHelper.getResourceAsImageView(Initializer.class, "logo.png"));
            NbAboutFx nbAboutFx = new NbAboutFx(aboutModel);
            nbAboutFx.display();
        });

        //quit
        mSysQuitAction = new FxActionSwing(Dict.QUIT.toString(), () -> {
            Actions.forID("File", "se.trixon.almond.nbp.actions.QuitAction").actionPerformed(null);
        });
        mSysQuitAction.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN));
    }

    private void initListeners() {
        SwingUtilities.invokeLater(() -> {
            final JFrame frame = (JFrame) Almond.getFrame();
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowActivated(WindowEvent e) {
                    final boolean fullscreen = frame.isUndecorated();
                    mOptions.setFullscreen(fullscreen);
                    mSysViewFullscreenAction.setSelected(fullscreen);
                }
            });
        });

        mOptions.getPreferences().addPreferenceChangeListener((PreferenceChangeEvent evt) -> {
            switch (evt.getKey()) {
                case MOptions.KEY_MAP_ONLY:
                    mSysViewMapAction.setSelected(mOptions.isMapOnly());
                    break;

                default:
                    break;
            }
        });

        Mapton.getGlobalState().addListener((GlobalStateChangeEvent evt) -> {
            Platform.runLater(() -> {
                updateDocumentInfo(evt);
            });
        }, MKey.MAP_DOCUMENT_INFO);

        Mapton.getGlobalState().addListener((GlobalStateChangeEvent evt) -> {
            Platform.runLater(() -> {
                Notifications notifications = evt.getValue();
                notifications.owner(AppToolBar.this).position(Pos.TOP_RIGHT);

                switch (evt.getKey()) {
                    case MKey.NOTIFICATION:
                        notifications.show();
                        break;

                    case MKey.NOTIFICATION_CONFIRM:
                        notifications.showConfirm();
                        break;

                    case MKey.NOTIFICATION_ERROR:
                        notifications.showError();
                        break;

                    case MKey.NOTIFICATION_INFORMATION:
                        notifications.showInformation();
                        break;

                    case MKey.NOTIFICATION_WARNING:
                        notifications.showWarning();
                        break;

                    default:
                        throw new AssertionError();
                }
            });
        }, MKey.NOTIFICATION, MKey.NOTIFICATION_CONFIRM, MKey.NOTIFICATION_ERROR, MKey.NOTIFICATION_INFORMATION, MKey.NOTIFICATION_WARNING);
    }

    private void initPopOver(PopOver popOver, String title, Node content) {
        popOver.setTitle(title);
        popOver.setContentNode(content);
        popOver.setArrowLocation(PopOver.ArrowLocation.TOP_LEFT);
        popOver.setHeaderAlwaysVisible(true);
        popOver.setCloseButtonEnabled(false);
        popOver.setDetachable(false);
        popOver.setAnimated(false);
        popOver.setOnHiding((windowEvent -> {
            mPopoverClosingTimes.put(popOver, System.currentTimeMillis());
        }));
        mPopOvers.add(popOver);
    }

    private void initPopOvers() {
        mBookmarkPopOver = new PopOver();
        initPopOver(mBookmarkPopOver, Dict.BOOKMARKS.toString(), new BookmarkView());

        mLayerPopOver = new PopOver();
        initPopOver(mLayerPopOver, Dict.LAYERS.toString(), null);
        mLayerPopOver.setOnShowing((event) -> {
            mLayerPopOver.setContentNode(new LayerView());
        });

        mToolboxPopOver = new PopOver();
        initPopOver(mToolboxPopOver, Dict.TOOLBOX.toString(), new ToolboxView());

        mStylePopOver = new PopOver();
        initPopOver(mStylePopOver, String.format("%s & %s", Dict.TYPE.toString(), Dict.STYLE.toString()), new BorderPane());

        Platform.runLater(() -> {
            mAttributionPopOver = new PopOver();
            mAttributionView = new AttributionView(mAttributionPopOver);
            initPopOver(mAttributionPopOver, Dict.COPYRIGHT.toString(), mAttributionView);
            mAttributionPopOver.setArrowLocation(PopOver.ArrowLocation.TOP_RIGHT);
        });
    }

    private boolean shouldOpen(PopOver popOver) {
        return System.currentTimeMillis() - mPopoverClosingTimes.getOrDefault(popOver, 0L) > 200;
    }

    private void tooglePopOver(PopOver popOver, Action action) {
        Platform.runLater(() -> {
            if (popOver.isShowing()) {
                popOver.hide();
            } else {
                mPopOvers.forEach((item) -> {
                    item.hide();
                });

                getItems().stream()
                        .filter((item) -> (item instanceof ButtonBase))
                        .map((item) -> (ButtonBase) item)
                        .filter((buttonBase) -> (buttonBase.getOnAction() == action))
                        .forEachOrdered((buttonBase) -> {
                            buttonBase.fire();
                        });
            }
        });
    }

    private void updateDocumentInfo(GlobalStateChangeEvent evt) {
        MDocumentInfo documentInfo = evt.getValue();
        mAttributionAction.setDisabled(false);
        mStyleAction.setText(documentInfo.getName());
        mAttributionView.updateDocumentInfo(documentInfo);
    }

    private boolean usePopOver() {
        return mOptions.isPreferPopover() || mOptions.isMapOnly();
    }
}
