package player.model;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AudioFiles {
	public static final List<String> AUDIO_FILE_EXTENSIONS = Arrays.asList(
			"mp3", "ogg", "wav", "aif", "aiff"
			);


	private  AudioFiles() {}


	public static List<File> allAudioFilesIn(File file) {
		return Arrays.asList(file.listFiles(f-> isAudioFile(f)));
	}

	public static boolean isAudioFile(File file) {
		return isAudioFile(file.getName());
	}

	public static boolean isAudioFile(String filename) {
		return AUDIO_FILE_EXTENSIONS.stream().anyMatch(ext -> filename.toLowerCase().endsWith("."+ext));
	}

	public static List<File> trim(List<File> arbitraryFileList) {
		return arbitraryFileList.stream().filter(file -> isAudioFile(file)).collect(Collectors.toList());
	}


	/**
	 * Replaces directories with their subfiles (not subfolders).
	 * @param files
	 * @return
	 */
	public static List<File> unfold(List<File> files) {
		List<File> result = new ArrayList<>();
		for(File file : files) {
			if(file.isDirectory()) {
				result.addAll(Arrays.asList(file.listFiles(new FileFilter() {
					@Override
					public boolean accept(File pathname) {
						return !pathname.isDirectory();
					}
				})));
			}
			else result.add(file);
		}
		return result;
	}
}
