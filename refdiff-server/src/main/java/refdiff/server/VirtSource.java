package refdiff.server;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import refdiff.core.io.SourceFile;
import refdiff.core.io.SourceFileSet;

public class VirtSource extends SourceFileSet {

	Map<SourceFile, String> files = new HashMap<>();
	File folder;

	public VirtSource(File folder, Map<String, String> files) {
		super(new ArrayList<>());
		List<SourceFile> l = getSourceFiles();
		for (Map.Entry<String, String> p : files.entrySet()) {
			SourceFile x = new SourceFile(Paths.get(p.getKey()));
			l.add(x);
			this.files.put(x, p.getValue());
		}
		this.folder = folder;
	}

	@Override
	public String readContent(SourceFile sourceFile) throws IOException {
		String r = files.get(sourceFile);
		if (r == null) {
			return "";
			// throw new IOException("no such files");
		}
		return r;
	}

	@Override
	public String describeLocation(SourceFile sourceFile) {
		return sourceFile.getPath();
	}

	@Override
	public Optional<Path> getBasePath() {
		return Optional.ofNullable(folder.toPath());
	}
	
	@Override
	public void materializeAt(Path folderPath) throws IOException {
		File folder = folderPath.toAbsolutePath().toFile();
		if (folder.exists() || folder.mkdirs()) {
			for (SourceFile sf : getSourceFiles()) {
				File destinationFile = new File(folder, sf.getPath());
				if (!destinationFile.exists()) {
					byte[] content = files.get(sf).getBytes();
					Files.createDirectories(destinationFile.getParentFile().toPath());
					Files.write(destinationFile.toPath(), content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);					
				}
			}
		} else {
			throw new IOException("Failed to create directory " + folderPath);
		}
	}
	
	@Override
	public void materializeAtBase(Path baseFolderPath) throws IOException {
		Path tmp = baseFolderPath.toAbsolutePath().resolve(this.folder.toPath().toAbsolutePath());
		materializeAt(tmp);
	}
}
