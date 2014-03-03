package org.fiteagle.core;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.fiteagle.api.core.IResourceRepository;

@Stateless
@Remote(IResourceRepository.class)
public class ResourceRepository implements IResourceRepository {

	private final static Logger LOGGER = Logger
			.getLogger(ResourceRepository.class.toString());

	public String listResources() {
		final String filename = "dummy-answer.ttl";
		Path file = getPath(filename);
		String content = "";

		try {
			content = getContent(file);
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, e.getLocalizedMessage());
		}

		return content;
	}

	private String getContent(Path file) throws IOException {
		String content = "";
		List<String> contentList = Files.readAllLines(file,
				Charset.forName("UTF-8"));
		for (String line : contentList) {
			content += line + "\n";
		}
		return content;
	}

	private Path getPath(final String filename) {
		URL url = this.getClass().getClassLoader().getResource(filename);
		String path = url.getPath();
		Path file = FileSystems.getDefault().getPath(path);
		return file;
	}
}
