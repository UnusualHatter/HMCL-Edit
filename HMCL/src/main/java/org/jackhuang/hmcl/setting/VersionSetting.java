/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2021  huangyuhui <huanghongxun2008@126.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.jackhuang.hmcl.setting;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.*;
import org.jackhuang.hmcl.game.*;
import org.jackhuang.hmcl.java.JavaManager;
import org.jackhuang.hmcl.util.Lang;
import org.jackhuang.hmcl.util.StringUtils;
import org.jackhuang.hmcl.util.javafx.ObservableHelper;
import org.jackhuang.hmcl.util.javafx.PropertyUtils;
import org.jackhuang.hmcl.java.JavaRuntime;
import org.jackhuang.hmcl.util.platform.SystemInfo;
import org.jackhuang.hmcl.util.versioning.GameVersionNumber;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.jackhuang.hmcl.util.DataSizeUnit.MEGABYTES;
import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/**
 * @author huangyuhui
 */
@JsonAdapter(VersionSetting.Serializer.class)
public final class VersionSetting implements Cloneable, Observable {

    private static final int SUGGESTED_MEMORY;

    static {
        double totalMemoryMB = MEGABYTES.convertFromBytes(SystemInfo.getTotalMemorySize());
        SUGGESTED_MEMORY = totalMemoryMB >= 32768
                ? 8192
                : Integer.max((int) (Math.round(totalMemoryMB / 4.0 / 128.0) * 128), 256);
    }

    private final transient ObservableHelper helper = new ObservableHelper(this);

    public VersionSetting() {
        PropertyUtils.attachListener(this, helper);
    }

    private final BooleanProperty usesGlobalProperty = new SimpleBooleanProperty(this, "usesGlobal", true);

    public BooleanProperty usesGlobalProperty() {
        return usesGlobalProperty;
    }

    /**
     * HMCL Version Settings have been divided into 2 parts.
     * 1. Global settings.
     * 2. Version settings.
     * If a version claims that it uses global settings, its version setting will be disabled.
     * <p>
     * Defaults false because if one version uses global first, custom version file will not be generated.
     */
    public boolean isUsesGlobal() {
        return usesGlobalProperty.get();
    }

    public void setUsesGlobal(boolean usesGlobal) {
        usesGlobalProperty.set(usesGlobal);
    }

    // java

    private final ObjectProperty<JavaVersionType> javaVersionTypeProperty = new SimpleObjectProperty<>(this, "javaVersionType", JavaVersionType.AUTO);

    public ObjectProperty<JavaVersionType> javaVersionTypeProperty() {
        return javaVersionTypeProperty;
    }

    public JavaVersionType getJavaVersionType() {
        return javaVersionTypeProperty.get();
    }

    public void setJavaVersionType(JavaVersionType javaVersionType) {
        javaVersionTypeProperty.set(javaVersionType);
    }

    private final StringProperty javaVersionProperty = new SimpleStringProperty(this, "javaVersion", "");

    public StringProperty javaVersionProperty() {
        return javaVersionProperty;
    }

    public String getJavaVersion() {
        return javaVersionProperty.get();
    }

    public void setJavaVersion(String java) {
        javaVersionProperty.set(java);
    }

    public void setUsesCustomJavaDir() {
        setJavaVersionType(JavaVersionType.CUSTOM);
        setJavaVersion("");
        setDefaultJavaPath(null);
    }

    public void setJavaAutoSelected() {
        setJavaVersionType(JavaVersionType.AUTO);
        setJavaVersion("");
        setDefaultJavaPath(null);
    }

    private final StringProperty defaultJavaPathProperty = new SimpleStringProperty(this, "defaultJavaPath", "");

    /**
     * Path to Java executable, or null if user customizes java directory.
     * It's used to determine which JRE to use when multiple JREs match the selected Java version.
     */
    public String getDefaultJavaPath() {
        return defaultJavaPathProperty.get();
    }

    public StringProperty defaultJavaPathPropertyProperty() {
        return defaultJavaPathProperty;
    }

    public void setDefaultJavaPath(String defaultJavaPath) {
        defaultJavaPathProperty.set(defaultJavaPath);
    }

    /**
     * 0 - .minecraft/versions/&lt;version&gt;/natives/<br/>
     */
    private final ObjectProperty<NativesDirectoryType> nativesDirTypeProperty = new SimpleObjectProperty<>(this, "nativesDirType", NativesDirectoryType.VERSION_FOLDER);

    public ObjectProperty<NativesDirectoryType> nativesDirTypeProperty() {
        return nativesDirTypeProperty;
    }

    public NativesDirectoryType getNativesDirType() {
        return nativesDirTypeProperty.get();
    }

    public void setNativesDirType(NativesDirectoryType nativesDirType) {
        nativesDirTypeProperty.set(nativesDirType);
    }

    // Path to lwjgl natives directory

    private final StringProperty nativesDirProperty = new SimpleStringProperty(this, "nativesDirProperty", "");

    public StringProperty nativesDirProperty() {
        return nativesDirProperty;
    }

    public String getNativesDir() {
        return nativesDirProperty.get();
    }

    public void setNativesDir(String nativesDir) {
        nativesDirProperty.set(nativesDir);
    }

    private final StringProperty javaDirProperty = new SimpleStringProperty(this, "javaDir", "");

    public StringProperty javaDirProperty() {
        return javaDirProperty;
    }

    /**
     * User customized java directory or null if user uses system Java.
     */
    public String getJavaDir() {
        return javaDirProperty.get();
    }

    public void setJavaDir(String javaDir) {
        javaDirProperty.set(javaDir);
    }

    private final StringProperty wrapperProperty = new SimpleStringProperty(this, "wrapper", "");

    public StringProperty wrapperProperty() {
        return wrapperProperty;
    }

    /**
     * The command to launch java, i.e. optirun.
     */
    public String getWrapper() {
        return wrapperProperty.get();
    }

    public void setWrapper(String wrapper) {
        wrapperProperty.set(wrapper);
    }

    private final StringProperty permSizeProperty = new SimpleStringProperty(this, "permSize", "");

    public StringProperty permSizeProperty() {
        return permSizeProperty;
    }

    /**
     * The permanent generation size of JVM garbage collection.
     */
    public String getPermSize() {
        return permSizeProperty.get();
    }

    public void setPermSize(String permSize) {
        permSizeProperty.set(permSize);
    }

    private final IntegerProperty maxMemoryProperty = new SimpleIntegerProperty(this, "maxMemory", SUGGESTED_MEMORY);

    public IntegerProperty maxMemoryProperty() {
        return maxMemoryProperty;
    }

    /**
     * The maximum memory/MB that JVM can allocate for heap.
     */
    public int getMaxMemory() {
        return maxMemoryProperty.get();
    }

    public void setMaxMemory(int maxMemory) {
        maxMemoryProperty.set(maxMemory);
    }

    /**
     * The minimum memory that JVM can allocate for heap.
     */
    private final ObjectProperty<Integer> minMemoryProperty = new SimpleObjectProperty<>(this, "minMemory", null);

    public ObjectProperty<Integer> minMemoryProperty() {
        return minMemoryProperty;
    }

    public Integer getMinMemory() {
        return minMemoryProperty.get();
    }

    public void setMinMemory(Integer minMemory) {
        minMemoryProperty.set(minMemory);
    }

    private final BooleanProperty autoMemory = new SimpleBooleanProperty(this, "autoMemory", true);

    public boolean isAutoMemory() {
        return autoMemory.get();
    }

    public BooleanProperty autoMemoryProperty() {
        return autoMemory;
    }

    public void setAutoMemory(boolean autoMemory) {
        this.autoMemory.set(autoMemory);
    }

    private final StringProperty preLaunchCommandProperty = new SimpleStringProperty(this, "precalledCommand", "");

    public StringProperty preLaunchCommandProperty() {
        return preLaunchCommandProperty;
    }

    /**
     * The command that will be executed before launching the Minecraft.
     * Operating system relevant.
     */
    public String getPreLaunchCommand() {
        return preLaunchCommandProperty.get();
    }

    public void setPreLaunchCommand(String preLaunchCommand) {
        preLaunchCommandProperty.set(preLaunchCommand);
    }

    private final StringProperty postExitCommand = new SimpleStringProperty(this, "postExitCommand", "");

    public StringProperty postExitCommandProperty() {
        return postExitCommand;
    }

    /**
     * The command that will be executed after game exits.
     * Operating system relevant.
     */
    public String getPostExitCommand() {
        return postExitCommand.get();
    }

    public void setPostExitCommand(String postExitCommand) {
        this.postExitCommand.set(postExitCommand);
    }

    // options

    private final StringProperty javaArgsProperty = new SimpleStringProperty(this, "javaArgs", "");

    public StringProperty javaArgsProperty() {
        return javaArgsProperty;
    }

    /**
     * The user customized arguments passed to JVM.
     */
    public String getJavaArgs() {
        return javaArgsProperty.get();
    }

    public void setJavaArgs(String javaArgs) {
        javaArgsProperty.set(javaArgs);
    }

    private final StringProperty minecraftArgsProperty = new SimpleStringProperty(this, "minecraftArgs", "");

    public StringProperty minecraftArgsProperty() {
        return minecraftArgsProperty;
    }

    /**
     * The user customized arguments passed to Minecraft.
     */
    public String getMinecraftArgs() {
        return minecraftArgsProperty.get();
    }

    public void setMinecraftArgs(String minecraftArgs) {
        minecraftArgsProperty.set(minecraftArgs);
    }

    private final StringProperty environmentVariablesProperty = new SimpleStringProperty(this, "environmentVariables", "");

    public StringProperty environmentVariablesProperty() {
        return environmentVariablesProperty;
    }

    public String getEnvironmentVariables() {
        return environmentVariablesProperty.get();
    }

    public void setEnvironmentVariables(String env) {
        environmentVariablesProperty.set(env);
    }

    private final BooleanProperty noJVMArgsProperty = new SimpleBooleanProperty(this, "noJVMArgs", false);

    public BooleanProperty noJVMArgsProperty() {
        return noJVMArgsProperty;
    }

    /**
     * True if disallow HMCL use default JVM arguments.
     */
    public boolean isNoJVMArgs() {
        return noJVMArgsProperty.get();
    }

    public void setNoJVMArgs(boolean noJVMArgs) {
        noJVMArgsProperty.set(noJVMArgs);
    }

    private final BooleanProperty notCheckJVMProperty = new SimpleBooleanProperty(this, "notCheckJVM", false);

    public BooleanProperty notCheckJVMProperty() {
        return notCheckJVMProperty;
    }

    /**
     * True if HMCL does not check JVM validity.
     */
    public boolean isNotCheckJVM() {
        return notCheckJVMProperty.get();
    }

    public void setNotCheckJVM(boolean notCheckJVM) {
        notCheckJVMProperty.set(notCheckJVM);
    }

    private final BooleanProperty notCheckGameProperty = new SimpleBooleanProperty(this, "notCheckGame", false);

    public BooleanProperty notCheckGameProperty() {
        return notCheckGameProperty;
    }

    /**
     * True if HMCL does not check game's completeness.
     */
    public boolean isNotCheckGame() {
        return notCheckGameProperty.get();
    }

    public void setNotCheckGame(boolean notCheckGame) {
        notCheckGameProperty.set(notCheckGame);
    }

    private final BooleanProperty notPatchNativesProperty = new SimpleBooleanProperty(this, "notPatchNatives", false);

    public BooleanProperty notPatchNativesProperty() {
        return notPatchNativesProperty;
    }

    public boolean isNotPatchNatives() {
        return notPatchNativesProperty.get();
    }

    public void setNotPatchNatives(boolean notPatchNatives) {
        notPatchNativesProperty.set(notPatchNatives);
    }

    private final BooleanProperty showLogsProperty = new SimpleBooleanProperty(this, "showLogs", true);

    public BooleanProperty showLogsProperty() {
        return showLogsProperty;
    }

    /**
     * True if show the logs after game launched.
     */
    public boolean isShowLogs() {
        return showLogsProperty.get();
    }

    public void setShowLogs(boolean showLogs) {
        showLogsProperty.set(showLogs);
    }

    // Minecraft settings.

    private final StringProperty serverIpProperty = new SimpleStringProperty(this, "serverIp", "");

    public StringProperty serverIpProperty() {
        return serverIpProperty;
    }

    /**
     * The server ip that will be entered after Minecraft successfully loaded ly.
     * <p>
     * Format: ip:port or without port.
     */
    public String getServerIp() {
        return serverIpProperty.get();
    }

    public void setServerIp(String serverIp) {
        serverIpProperty.set(serverIp);
    }


    private final BooleanProperty fullscreenProperty = new SimpleBooleanProperty(this, "fullscreen", false);

    public BooleanProperty fullscreenProperty() {
        return fullscreenProperty;
    }

    /**
     * True if Minecraft started in fullscreen mode.
     */
    public boolean isFullscreen() {
        return fullscreenProperty.get();
    }

    public void setFullscreen(boolean fullscreen) {
        fullscreenProperty.set(fullscreen);
    }

    private final IntegerProperty widthProperty = new SimpleIntegerProperty(this, "width", 854);

    public IntegerProperty widthProperty() {
        return widthProperty;
    }

    /**
     * The width of Minecraft window, defaults 800.
     * <p>
     * The field saves int value.
     * String type prevents unexpected value from JsonParseException.
     * We can only reset this field instead of recreating the whole setting file.
     */
    public int getWidth() {
        return widthProperty.get();
    }

    public void setWidth(int width) {
        widthProperty.set(width);
    }

    private final IntegerProperty heightProperty = new SimpleIntegerProperty(this, "height", 480);

    public IntegerProperty heightProperty() {
        return heightProperty;
    }

    /**
     * The height of Minecraft window, defaults 480.
     * <p>
     * The field saves int value.
     * String type prevents unexpected value from JsonParseException.
     * We can only reset this field instead of recreating the whole setting file.
     */
    public int getHeight() {
        return heightProperty.get();
    }

    public void setHeight(int height) {
        heightProperty.set(height);
    }

    /**
     * 0 - .minecraft<br/>
     * 1 - .minecraft/versions/&lt;version&gt;/<br/>
     */
    private final ObjectProperty<GameDirectoryType> gameDirTypeProperty = new SimpleObjectProperty<>(this, "gameDirType", GameDirectoryType.ROOT_FOLDER);

    public ObjectProperty<GameDirectoryType> gameDirTypeProperty() {
        return gameDirTypeProperty;
    }

    public GameDirectoryType getGameDirType() {
        return gameDirTypeProperty.get();
    }

    public void setGameDirType(GameDirectoryType gameDirType) {
        gameDirTypeProperty.set(gameDirType);
    }

    /**
     * Your custom gameDir
     */
    private final StringProperty gameDirProperty = new SimpleStringProperty(this, "gameDir", "modpack");

    public StringProperty gameDirProperty() {
        return gameDirProperty;
    }

    public String getGameDir() {
        return "modpack";
    }

    public void setGameDir(String gameDir) {
    gameDirProperty.set("modpack");
    }

    private final ObjectProperty<ProcessPriority> processPriorityProperty = new SimpleObjectProperty<>(this, "processPriority", ProcessPriority.HIGH);

    public ObjectProperty<ProcessPriority> processPriorityProperty() {
        return processPriorityProperty;
    }

    public ProcessPriority getProcessPriority() {
        return processPriorityProperty.get();
    }

    public void setProcessPriority(ProcessPriority processPriority) {
        processPriorityProperty.set(processPriority);
    }

    private final ObjectProperty<Renderer> rendererProperty = new SimpleObjectProperty<>(this, "renderer", Renderer.DEFAULT);

    public Renderer getRenderer() {
        return rendererProperty.get();
    }

    public ObjectProperty<Renderer> rendererProperty() {
        return rendererProperty;
    }

    public void setRenderer(Renderer renderer) {
        this.rendererProperty.set(renderer);
    }

    private final BooleanProperty useNativeGLFW = new SimpleBooleanProperty(this, "nativeGLFW", false);

    public boolean isUseNativeGLFW() {
        return useNativeGLFW.get();
    }

    public BooleanProperty useNativeGLFWProperty() {
        return useNativeGLFW;
    }

    public void setUseNativeGLFW(boolean useNativeGLFW) {
        this.useNativeGLFW.set(useNativeGLFW);
    }

    private final BooleanProperty useNativeOpenAL = new SimpleBooleanProperty(this, "nativeOpenAL", false);

    public boolean isUseNativeOpenAL() {
        return useNativeOpenAL.get();
    }

    public BooleanProperty useNativeOpenALProperty() {
        return useNativeOpenAL;
    }

    public void setUseNativeOpenAL(boolean useNativeOpenAL) {
        this.useNativeOpenAL.set(useNativeOpenAL);
    }

    private final ObjectProperty<VersionIconType> versionIcon = new SimpleObjectProperty<>(this, "versionIcon", VersionIconType.DEFAULT);

    public VersionIconType getVersionIcon() {
        return versionIcon.get();
    }

    public ObjectProperty<VersionIconType> versionIconProperty() {
        return versionIcon;
    }

    public void setVersionIcon(VersionIconType versionIcon) {
        this.versionIcon.set(versionIcon);
    }

    // launcher settings

    /**
     * 0 - Close the launcher when the game starts.<br/>
     * 1 - Hide the launcher when the game starts.<br/>
     * 2 - Keep the launcher open.<br/>
     */
    private final ObjectProperty<LauncherVisibility> launcherVisibilityProperty = new SimpleObjectProperty<>(this, "launcherVisibility", LauncherVisibility.HIDE_AND_REOPEN);

    public ObjectProperty<LauncherVisibility> launcherVisibilityProperty() {
        return launcherVisibilityProperty;
    }

    public LauncherVisibility getLauncherVisibility() {
        return launcherVisibilityProperty.get();
    }

    public void setLauncherVisibility(LauncherVisibility launcherVisibility) {
        launcherVisibilityProperty.set(launcherVisibility);
    }

    public JavaRuntime getJava(GameVersionNumber gameVersion, Version version) throws InterruptedException {
        switch (getJavaVersionType()) {
            case DEFAULT:
                return JavaRuntime.getDefault();
            case AUTO:
                return JavaManager.findSuitableJava(gameVersion, version);
            case CUSTOM:
                try {
                    return JavaManager.getJava(Paths.get(getJavaDir()));
                } catch (IOException | InvalidPathException e) {
                    return null; // Custom Java not found
                }
            case VERSION: {
                String javaVersion = getJavaVersion();
                if (StringUtils.isBlank(javaVersion)) {
                    return JavaManager.findSuitableJava(gameVersion, version);
                }

                int majorVersion = -1;
                try {
                    majorVersion = Integer.parseInt(javaVersion);
                } catch (NumberFormatException ignored) {
                }

                if (majorVersion < 0) {
                    LOG.warning("Invalid Java version: " + javaVersion);
                    return null;
                }

                final int finalMajorVersion = majorVersion;
                Collection<JavaRuntime> allJava = JavaManager.getAllJava().stream()
                        .filter(it -> it.getParsedVersion() == finalMajorVersion)
                        .collect(Collectors.toList());
                return JavaManager.findSuitableJava(allJava, gameVersion, version);
            }
            case DETECTED: {
                String javaVersion = getJavaVersion();
                if (StringUtils.isBlank(javaVersion)) {
                    return JavaManager.findSuitableJava(gameVersion, version);
                }

                try {
                    String defaultJavaPath = getDefaultJavaPath();
                    if (StringUtils.isNotBlank(defaultJavaPath)) {
                        JavaRuntime java = JavaManager.getJava(Paths.get(defaultJavaPath).toRealPath());
                        if (java != null && java.getVersion().equals(javaVersion)) {
                            return java;
                        }
                    }
                } catch (IOException | InvalidPathException ignored) {
                }

                for (JavaRuntime java : JavaManager.getAllJava()) {
                    if (java.getVersion().equals(javaVersion)) {
                        return java;
                    }
                }

                return null;
            }
            default:
                throw new AssertionError("JavaVersionType: " + getJavaVersionType());
        }
    }

    @Override
    public void addListener(InvalidationListener listener) {
        helper.addListener(listener);
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        helper.removeListener(listener);
    }

    @Override
    public VersionSetting clone() {
        VersionSetting cloned = new VersionSetting();
        PropertyUtils.copyProperties(this, cloned);
        return cloned;
    }

    public static class Serializer implements JsonSerializer<VersionSetting>, JsonDeserializer<VersionSetting> {
        @Override
        public JsonElement serialize(VersionSetting src, Type typeOfSrc, JsonSerializationContext context) {
            if (src == null) return JsonNull.INSTANCE;
            JsonObject obj = new JsonObject();

            obj.addProperty("usesGlobal", src.isUsesGlobal());
            obj.addProperty("javaArgs", src.getJavaArgs());
            obj.addProperty("minecraftArgs", src.getMinecraftArgs());
            obj.addProperty("environmentVariables", src.getEnvironmentVariables());
            obj.addProperty("maxMemory", src.getMaxMemory() <= 0 ? SUGGESTED_MEMORY : src.getMaxMemory());
            obj.addProperty("minMemory", src.getMinMemory());
            obj.addProperty("autoMemory", src.isAutoMemory());
            obj.addProperty("permSize", src.getPermSize());
            obj.addProperty("width", src.getWidth());
            obj.addProperty("height", src.getHeight());
            obj.addProperty("javaDir", src.getJavaDir());
            obj.addProperty("precalledCommand", src.getPreLaunchCommand());
            obj.addProperty("postExitCommand", src.getPostExitCommand());
            obj.addProperty("serverIp", src.getServerIp());
            obj.addProperty("wrapper", src.getWrapper());
            obj.addProperty("fullscreen", src.isFullscreen());
            obj.addProperty("noJVMArgs", src.isNoJVMArgs());
            obj.addProperty("notCheckGame", src.isNotCheckGame());
            obj.addProperty("notCheckJVM", src.isNotCheckJVM());
            obj.addProperty("notPatchNatives", src.isNotPatchNatives());
            obj.addProperty("showLogs", src.isShowLogs());
            obj.addProperty("gameDir", src.getGameDir());
            obj.addProperty("launcherVisibility", src.getLauncherVisibility().ordinal());
            obj.addProperty("processPriority", src.getProcessPriority().ordinal());
            obj.addProperty("useNativeGLFW", src.isUseNativeGLFW());
            obj.addProperty("useNativeOpenAL", src.isUseNativeOpenAL());
            obj.addProperty("gameDirType", src.getGameDirType().ordinal());
            obj.addProperty("defaultJavaPath", src.getDefaultJavaPath());
            obj.addProperty("nativesDir", src.getNativesDir());
            obj.addProperty("nativesDirType", src.getNativesDirType().ordinal());
            obj.addProperty("versionIcon", src.getVersionIcon().ordinal());

            obj.addProperty("javaVersionType", src.getJavaVersionType().name());
            String java;
            switch (src.getJavaVersionType()) {
                case DEFAULT:
                    java = "Default";
                    break;
                case AUTO:
                    java = "Auto";
                    break;
                case CUSTOM:
                    java = "Custom";
                    break;
                default:
                    java = src.getJavaVersion();
                    break;
            }
            obj.addProperty("java", java);

            obj.addProperty("renderer", src.getRenderer().name());
            if (src.getRenderer() == Renderer.LLVMPIPE)
                obj.addProperty("useSoftwareRenderer", true);

            return obj;
        }

        private static <T> T getOrDefault(T[] values, JsonElement index, T defaultValue) {
            if (index == null)
                return defaultValue;

            int idx = index.getAsInt();
            return idx >= 0 && idx < values.length ? values[idx] : defaultValue;
        }

        @Override
        public VersionSetting deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (!(json instanceof JsonObject))
                return null;
            JsonObject obj = (JsonObject) json;

            int maxMemoryN = parseJsonPrimitive(Optional.ofNullable(obj.get("maxMemory")).map(JsonElement::getAsJsonPrimitive).orElse(null), SUGGESTED_MEMORY);
            if (maxMemoryN <= 0) maxMemoryN = SUGGESTED_MEMORY;

            VersionSetting vs = new VersionSetting();

            vs.setUsesGlobal(Optional.ofNullable(obj.get("usesGlobal")).map(JsonElement::getAsBoolean).orElse(false));
            vs.setJavaArgs(Optional.ofNullable(obj.get("javaArgs")).map(JsonElement::getAsString).orElse(""));
            vs.setMinecraftArgs(Optional.ofNullable(obj.get("minecraftArgs")).map(JsonElement::getAsString).orElse(""));
            vs.setEnvironmentVariables(Optional.ofNullable(obj.get("environmentVariables")).map(JsonElement::getAsString).orElse(""));
            vs.setMaxMemory(maxMemoryN);
            vs.setMinMemory(Optional.ofNullable(obj.get("minMemory")).map(JsonElement::getAsInt).orElse(null));
            vs.setAutoMemory(Optional.ofNullable(obj.get("autoMemory")).map(JsonElement::getAsBoolean).orElse(true));
            vs.setPermSize(Optional.ofNullable(obj.get("permSize")).map(JsonElement::getAsString).orElse(""));
            vs.setWidth(Optional.ofNullable(obj.get("width")).map(JsonElement::getAsJsonPrimitive).map(this::parseJsonPrimitive).orElse(0));
            vs.setHeight(Optional.ofNullable(obj.get("height")).map(JsonElement::getAsJsonPrimitive).map(this::parseJsonPrimitive).orElse(0));
            vs.setJavaDir(Optional.ofNullable(obj.get("javaDir")).map(JsonElement::getAsString).orElse(""));
            vs.setPreLaunchCommand(Optional.ofNullable(obj.get("precalledCommand")).map(JsonElement::getAsString).orElse(""));
            vs.setPostExitCommand(Optional.ofNullable(obj.get("postExitCommand")).map(JsonElement::getAsString).orElse(""));
            vs.setServerIp(Optional.ofNullable(obj.get("serverIp")).map(JsonElement::getAsString).orElse(""));
            vs.setWrapper(Optional.ofNullable(obj.get("wrapper")).map(JsonElement::getAsString).orElse(""));
            vs.setGameDir(Optional.ofNullable(obj.get("gameDir")).map(JsonElement::getAsString).orElse(""));
            vs.setNativesDir(Optional.ofNullable(obj.get("nativesDir")).map(JsonElement::getAsString).orElse(""));
            vs.setFullscreen(Optional.ofNullable(obj.get("fullscreen")).map(JsonElement::getAsBoolean).orElse(false));
            vs.setNoJVMArgs(Optional.ofNullable(obj.get("noJVMArgs")).map(JsonElement::getAsBoolean).orElse(false));
            vs.setNotCheckGame(Optional.ofNullable(obj.get("notCheckGame")).map(JsonElement::getAsBoolean).orElse(false));
            vs.setNotCheckJVM(Optional.ofNullable(obj.get("notCheckJVM")).map(JsonElement::getAsBoolean).orElse(false));
            vs.setNotPatchNatives(Optional.ofNullable(obj.get("notPatchNatives")).map(JsonElement::getAsBoolean).orElse(false));
            vs.setShowLogs(Optional.ofNullable(obj.get("showLogs")).map(JsonElement::getAsBoolean).orElse(true));
            vs.setLauncherVisibility(getOrDefault(LauncherVisibility.values(), obj.get("launcherVisibility"), LauncherVisibility.HIDE_AND_REOPEN));
            vs.setProcessPriority(getOrDefault(ProcessPriority.values(), obj.get("processPriority"), ProcessPriority.NORMAL));
            vs.setUseNativeGLFW(Optional.ofNullable(obj.get("useNativeGLFW")).map(JsonElement::getAsBoolean).orElse(false));
            vs.setUseNativeOpenAL(Optional.ofNullable(obj.get("useNativeOpenAL")).map(JsonElement::getAsBoolean).orElse(false));
            vs.setGameDirType(getOrDefault(GameDirectoryType.values(), obj.get("gameDirType"), GameDirectoryType.ROOT_FOLDER));
            vs.setDefaultJavaPath(Optional.ofNullable(obj.get("defaultJavaPath")).map(JsonElement::getAsString).orElse(null));
            vs.setNativesDirType(getOrDefault(NativesDirectoryType.values(), obj.get("nativesDirType"), NativesDirectoryType.VERSION_FOLDER));
            vs.setVersionIcon(getOrDefault(VersionIconType.values(), obj.get("versionIcon"), VersionIconType.DEFAULT));

            if (obj.get("javaVersionType") != null) {
                JavaVersionType javaVersionType = parseJsonPrimitive(obj.getAsJsonPrimitive("javaVersionType"), JavaVersionType.class, JavaVersionType.AUTO);
                vs.setJavaVersionType(javaVersionType);
                vs.setJavaVersion(Optional.ofNullable(obj.get("java")).map(JsonElement::getAsString).orElse(null));
            } else {
                String java = Optional.ofNullable(obj.get("java")).map(JsonElement::getAsString).orElse("");
                switch (java) {
                    case "Default":
                        vs.setJavaVersionType(JavaVersionType.DEFAULT);
                        break;
                    case "Auto":
                        vs.setJavaVersionType(JavaVersionType.AUTO);
                        break;
                    case "Custom":
                        vs.setJavaVersionType(JavaVersionType.CUSTOM);
                        break;
                    default:
                        vs.setJavaVersion(java);
                }
            }

            vs.setRenderer(Optional.ofNullable(obj.get("renderer")).map(JsonElement::getAsString)
                    .flatMap(name -> {
                        try {
                            return Optional.of(Renderer.valueOf(name.toUpperCase(Locale.ROOT)));
                        } catch (IllegalArgumentException ignored) {
                            return Optional.empty();
                        }
                    }).orElseGet(() -> {
                        boolean useSoftwareRenderer = Optional.ofNullable(obj.get("useSoftwareRenderer")).map(JsonElement::getAsBoolean).orElse(false);
                        return useSoftwareRenderer ? Renderer.LLVMPIPE : Renderer.DEFAULT;
                    }));

            return vs;
        }

        private int parseJsonPrimitive(JsonPrimitive primitive) {
            return parseJsonPrimitive(primitive, 0);
        }

        private int parseJsonPrimitive(JsonPrimitive primitive, int defaultValue) {
            if (primitive == null)
                return defaultValue;
            else if (primitive.isNumber())
                return primitive.getAsInt();
            else
                return Lang.parseInt(primitive.getAsString(), defaultValue);
        }

        private <E extends Enum<E>> E parseJsonPrimitive(JsonPrimitive primitive, Class<E> clazz, E defaultValue) {
            if (primitive == null)
                return defaultValue;
            else {
                E[] enumConstants = clazz.getEnumConstants();
                if (primitive.isNumber()) {
                    int index = primitive.getAsInt();
                    return index >= 0 && index < enumConstants.length ? enumConstants[index] : defaultValue;
                } else {
                    String name = primitive.getAsString();
                    for (E enumConstant : enumConstants) {
                        if (enumConstant.name().equalsIgnoreCase(name)) {
                            return enumConstant;
                        }
                    }
                    return defaultValue;
                }
            }
        }
    }
}
