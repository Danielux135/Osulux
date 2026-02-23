package com.osuplayer.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.osuplayer.config.ConfigManager;
import com.osuplayer.lang.I18n;
import com.osuplayer.lang.LanguageBindings;
import com.osuplayer.lang.LanguageManager;
import com.osuplayer.lang.LanguagePack;
import com.osuplayer.dependencies.IconDependencyProvider;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.StringConverter;


public class SettingsDialog {

    private static final List<ThemeOption> THEME_PRESETS = List.of(
        new ThemeOption("light", "Blanco"),
        new ThemeOption("blue", "Azul claro"),
        new ThemeOption("light-red", "Rojo claro"),
        new ThemeOption("light-orange", "Naranja claro"),
        new ThemeOption("dark", "Oscuro"),
        new ThemeOption("orange", "Naranja"),
        new ThemeOption("red", "Rojo"),
        new ThemeOption("green", "Verde")
    );

    private final ConfigManager configManager;
    private final LanguageManager languageManager = LanguageManager.getInstance();
    private final Consumer<String> themeChangeListener;
    private Consumer<Boolean> historyRetentionChangeListener;
    private final List<Image> iconImages = new ArrayList<>();
    private static Image fallbackIcon;
    private Stage stage;
    private String currentThemeId;
    private CheckBox deleteConfirmationCheckBox;
    private ComboBox<LanguagePack> languageCombo;
    private ObservableList<LanguagePack> languageOptions;
    private ComboBox<ThemeOption> themeCombo;
    private ObservableList<ThemeOption> themeComboItems;
    private CheckBox historyRetentionCheckBox;
    private boolean updatingThemeSelection;
    private Runnable languageChangeListener;

    public SettingsDialog(ConfigManager configManager, Consumer<String> themeChangeListener) {
        this.configManager = configManager;
        this.themeChangeListener = themeChangeListener == null ? theme -> {} : themeChangeListener;
    }

    public void setIconImages(List<Image> icons) {
        iconImages.clear();
        if (icons != null && !icons.isEmpty()) {
            iconImages.addAll(icons);
        }
        applyIconsToStage(stage);
    }

    public void setOnHistoryRetentionChanged(Consumer<Boolean> listener) {
        this.historyRetentionChangeListener = listener;
    }

    public void setOnLanguageChanged(Runnable listener) {
        this.languageChangeListener = listener;
    }

    public void applyTheme(String themeId) {
        this.currentThemeId = themeId;
        if (stage != null) {
            applyThemeToScene(stage.getScene());
        }
        syncThemeCombo();
    }

    public void show(Window owner) {
        if (stage == null) {
            stage = buildStage(owner);
        } else if (stage.getOwner() == null && owner != null) {
            stage.initOwner(owner);
        }
        refreshValues();
        stage.show();
        stage.toFront();
    }

    private Stage buildStage(Window owner) {
        Stage dialog = new Stage(StageStyle.UTILITY);
        LanguageBindings.bindStageTitle(dialog, "Configuración");
        dialog.initModality(Modality.NONE); 
        if (owner != null) {
            dialog.initOwner(owner);
        }
        Label titleLabel = new Label();
        LanguageBindings.bindLabeled(titleLabel, "Configuración");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        GridPane form = new GridPane();
        form.setHgap(14);
        form.setVgap(16);
        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setPercentWidth(55);
        ColumnConstraints controlColumn = new ColumnConstraints();
        controlColumn.setPercentWidth(45);
        controlColumn.setFillWidth(true);
        form.getColumnConstraints().addAll(labelColumn, controlColumn);

        Label languageLabel = new Label();
        LanguageBindings.bindLabeled(languageLabel, "Lenguaje:");
        languageCombo = createLanguageCombo();
        form.add(languageLabel, 0, 0);
        form.add(languageCombo, 1, 0);

        Label themeLabel = new Label();
        LanguageBindings.bindLabeled(themeLabel, "Seleccionar tema");
        themeCombo = createThemeCombo();
        form.add(themeLabel, 0, 1);
        form.add(themeCombo, 1, 1);

        Label deleteLabel = new Label();
        LanguageBindings.bindLabeled(deleteLabel, "Activar ventana de alerta al eliminar canciones");
        deleteConfirmationCheckBox = new CheckBox();
        deleteConfirmationCheckBox.setFocusTraversable(false);
        deleteConfirmationCheckBox.selectedProperty().addListener((obs, oldVal, newVal) ->
            configManager.setBeatmapDeleteConfirmationEnabled(newVal)
        );
        form.add(deleteLabel, 0, 2);
        form.add(deleteConfirmationCheckBox, 1, 2);

        Label historyLabel = new Label();
        LanguageBindings.bindLabeled(historyLabel, "Mantener el historial al cerrar el programa");
        historyRetentionCheckBox = new CheckBox();
        historyRetentionCheckBox.setFocusTraversable(false);
        historyRetentionCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            configManager.setHistoryRetentionEnabled(newVal);
            if (historyRetentionChangeListener != null) {
                historyRetentionChangeListener.accept(newVal);
            }
        });
        form.add(historyLabel, 0, 3);
        form.add(historyRetentionCheckBox, 1, 3);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button closeButton = new Button();
        LanguageBindings.bindLabeled(closeButton, "Cerrar");
        closeButton.setOnAction(e -> dialog.close());
        HBox footer = new HBox(closeButton);
        footer.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(15, titleLabel, form, spacer, footer);
        root.setPadding(new Insets(18));

        double preferredWidth = 560;
        Scene scene = new Scene(root, preferredWidth, 320);
        dialog.setMinWidth(preferredWidth);
        dialog.setResizable(false); 
        dialog.setScene(scene);
        applyThemeToScene(scene);
        applyIconsToStage(dialog);
        dialog.setAlwaysOnTop(true);
        return dialog;
    }

    private ComboBox<LanguagePack> createLanguageCombo() {
        languageOptions = FXCollections.observableArrayList(languageManager.getAvailableLanguages());
        ComboBox<LanguagePack> combo = new ComboBox<>(languageOptions);
        combo.setPrefWidth(220);
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.setConverter(new StringConverter<>() {
            @Override
            public String toString(LanguagePack pack) {
                return pack == null ? "" : pack.displayName();
            }

            @Override
            public LanguagePack fromString(String string) {
                if (string == null) {
                    return null;
                }
                for (LanguagePack pack : languageOptions) {
                    if (string.equals(pack.displayName())) {
                        return pack;
                    }
                }
                return null;
            }
        });
        combo.setButtonCell(createLanguageCell());
        combo.setCellFactory(listView -> createLanguageCell());
        combo.valueProperty().addListener((obs, oldPack, newPack) -> {
            if (newPack == null) {
                return;
            }
            boolean changed = !newPack.id().equals(languageManager.getLanguageId());
            if (changed) {
                languageManager.setLanguage(newPack.id());
                configManager.setLanguage(newPack.id());
            }
            if (languageChangeListener != null && changed) {
                languageChangeListener.run();
            }
        });
        languageManager.languageIdProperty().addListener((obs, oldId, newId) -> syncLanguageCombo());
        return combo;
    }

    private ComboBox<ThemeOption> createThemeCombo() {
        themeComboItems = FXCollections.observableArrayList(THEME_PRESETS);
        ComboBox<ThemeOption> combo = new ComboBox<>(themeComboItems);
        combo.setPrefWidth(220);
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.setButtonCell(createThemeCell());
        combo.setCellFactory(listView -> createThemeCell());
        combo.valueProperty().addListener((obs, oldOption, newOption) -> {
            if (updatingThemeSelection || newOption == null) {
                return;
            }
            themeChangeListener.accept(newOption.id());
        });
        languageManager.languageIdProperty().addListener((obs, oldId, newId) -> refreshThemeComboTexts());
        return combo;
    }

    private void refreshValues() {
        if (languageCombo != null) {
            syncLanguageCombo();
        }
        if (themeCombo != null) {
            syncThemeCombo();
        }
        if (deleteConfirmationCheckBox != null) {
            deleteConfirmationCheckBox.setSelected(configManager.isBeatmapDeleteConfirmationEnabled());
        }
        if (historyRetentionCheckBox != null) {
            historyRetentionCheckBox.setSelected(configManager.isHistoryRetentionEnabled());
        }
    }

    private void syncLanguageCombo() {
        if (languageCombo == null || languageCombo.getItems() == null) {
            return;
        }
        String currentId = languageManager.getLanguageId();
        for (LanguagePack pack : languageCombo.getItems()) {
            if (pack.id().equals(currentId)) {
                languageCombo.getSelectionModel().select(pack);
                return;
            }
        }
        languageCombo.getSelectionModel().clearSelection();
    }

    private ListCell<LanguagePack> createLanguageCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(LanguagePack item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.displayName());
            }
        };
    }

    private ListCell<ThemeOption> createThemeCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(ThemeOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.displayName());
            }
        };
    }

    private void refreshThemeComboTexts() {
        if (themeCombo == null) {
            return;
        }
        ThemeOption selected = themeCombo.getSelectionModel().getSelectedItem();
        themeCombo.setButtonCell(createThemeCell());
        themeCombo.setCellFactory(listView -> createThemeCell());
        if (selected != null && themeCombo.getButtonCell() != null) {
            themeCombo.getButtonCell().setText(selected.displayName());
        }
    }

    private void syncThemeCombo() {
        if (themeCombo == null) {
            return;
        }
        ThemeOption match = findThemeOption(configManager.getTheme());
        updatingThemeSelection = true;
        if (match != null) {
            themeCombo.getSelectionModel().select(match);
        } else {
            themeCombo.getSelectionModel().clearSelection();
        }
        updatingThemeSelection = false;
    }

    private ThemeOption findThemeOption(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        List<ThemeOption> source = themeComboItems != null ? themeComboItems : THEME_PRESETS;
        for (ThemeOption option : source) {
            if (option.id().equals(id)) {
                return option;
            }
        }
        return null;
    }

    private void applyIconsToStage(Stage target) {
        if (target == null) {
            return;
        }
        List<Image> iconsToApply = new ArrayList<>();
        if (!iconImages.isEmpty()) {
            iconsToApply.addAll(iconImages);
        }
        Window owner = target.getOwner();
        if (iconsToApply.isEmpty() && owner instanceof Stage ownerStage && !ownerStage.getIcons().isEmpty()) {
            iconsToApply.addAll(ownerStage.getIcons());
        }
        if (iconsToApply.isEmpty()) {
            Image fallback = getFallbackIcon();
            if (fallback != null) {
                iconsToApply.add(fallback);
            }
        }
        if (!iconsToApply.isEmpty()) {
            target.getIcons().setAll(iconsToApply);
        }
    }

    private static Image getFallbackIcon() {
        if (fallbackIcon == null) {
            fallbackIcon = IconDependencyProvider.getOrNull();
        }
        return fallbackIcon;
    }

    private void applyThemeToScene(Scene scene) {
        ThemeHelper.applyTheme(scene, currentThemeId);
    }

    public void refreshLanguage() {
        refreshValues();
        refreshThemeComboTexts();
    }

    private record ThemeOption(String id, String labelKey) {
        String displayName() {
            return I18n.tr(labelKey);
        }
    }
}
