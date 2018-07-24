package no.tornado.tornadofx.idea;

import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.SourcePathsBuilder;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class TornadoFXModuleBuilder extends ModuleBuilder implements PluginIcons, SourcePathsBuilder {
    private List<Pair<String, String>> mySourcePaths;
    private final List<Pair<String, String>> myModuleLibraries = new ArrayList<>();
    private String myCompilerOutputPath;

	public void setupRootModel(ModifiableRootModel rootModel) throws ConfigurationException {
		final CompilerModuleExtension compilerModuleExtension = rootModel.getModuleExtension(CompilerModuleExtension.class);
		compilerModuleExtension.setExcludeOutput(true);
		rootModel.inheritSdk();

        ContentEntry contentEntry = doAddContentEntry(rootModel);
        if (contentEntry != null) {
            final List<Pair<String, String>> sourcePaths = getSourcePaths();

            if (sourcePaths != null) {
                for (final Pair<String, String> sourcePath : sourcePaths) {
                    String first = sourcePath.first;
                    new File(first).mkdirs();
                    final VirtualFile sourceRoot = LocalFileSystem.getInstance().refreshAndFindFileByPath(FileUtil.toSystemIndependentName(first));
                    if (sourceRoot != null) {
                        contentEntry.addSourceFolder(sourceRoot, false, sourcePath.second);
                    }
                }
            }
        }

        if (myCompilerOutputPath != null) {
            // should set only absolute paths
            String canonicalPath;
            try {
                canonicalPath = FileUtil.resolveShortWindowsName(myCompilerOutputPath);
            } catch (IOException e) {
                canonicalPath = myCompilerOutputPath;
            }
            compilerModuleExtension
                    .setCompilerOutputPath(VfsUtil.pathToUrl(FileUtil.toSystemIndependentName(canonicalPath)));
        } else {
            compilerModuleExtension.inheritCompilerOutputPath(true);
        }

        LibraryTable libraryTable = rootModel.getModuleLibraryTable();
        for (Pair<String, String> libInfo : myModuleLibraries) {
            final String moduleLibraryPath = libInfo.first;
            final String sourceLibraryPath = libInfo.second;
            Library library = libraryTable.createLibrary();
            Library.ModifiableModel modifiableModel = library.getModifiableModel();
            modifiableModel.addRoot(getUrlByPath(moduleLibraryPath), OrderRootType.CLASSES);
            if (sourceLibraryPath != null) {
                modifiableModel.addRoot(getUrlByPath(sourceLibraryPath), OrderRootType.SOURCES);
            }
            modifiableModel.commit();
        }

	}

	public ModuleType getModuleType() {
		return TornadoFXModuleType.getInstance();
	}

	public boolean isTemplateBased() {
		return true;
	}

	public boolean isTemplate() {
		return false;
	}

    @Override
    public List<Pair<String, String>> getSourcePaths() throws ConfigurationException {
        if (mySourcePaths == null) {
            final List<Pair<String, String>> paths = new ArrayList<>();

            Path src = Paths.get(getContentEntryPath()).resolve("src/main/kotlin");
            try {
                Files.createDirectories(src);
            } catch (IOException e) {
                e.printStackTrace();
            }
            paths.add(Pair.create(src.toString(), ""));

            return paths;
        }
        return mySourcePaths;
    }

    @Override
    public void setSourcePaths(List<Pair<String, String>> sourcePaths) {
        mySourcePaths = sourcePaths != null ? new ArrayList<>(sourcePaths) : null;
    }

    @Override
    public void addSourcePath(Pair<String, String> sourcePathInfo) {
        if (mySourcePaths == null) {
            mySourcePaths = new ArrayList<>();
        }
        mySourcePaths.add(sourcePathInfo);
    }

    private static String getUrlByPath(final String path) {
        return VfsUtil.getUrlForLibraryRoot(new File(path));
    }

}
