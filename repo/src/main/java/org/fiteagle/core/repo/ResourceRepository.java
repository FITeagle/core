package org.fiteagle.core.repo;

import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.fiteagle.api.core.IResourceRepository;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.tdb.TDBFactory;

public class ResourceRepository implements IResourceRepository {

	private static final String DEFAULT_SELECT = "SELECT * {?s ?p ?o} LIMIT 100";
	private static final String DUMMY_DATA = "dummy-answer.xml";
	private static final String REPODB_DIR = "repodb";
	private final static Logger LOGGER = Logger
			.getLogger(ResourceRepository.class.toString());
	private Dataset dataset;

	public ResourceRepository() {
		this.dataset = TDBFactory.createDataset(ResourceRepository.REPODB_DIR);
		// DatasetFactory.createMem();

		addDummyData();
	}

	private void addDummyData() {
		ResourceRepository.LOGGER.log(Level.INFO, "Loading dummy data from: "
				+ ResourceRepository.DUMMY_DATA);
		Model dummyModel = RDFDataMgr.loadModel(ResourceRepository.DUMMY_DATA,
				Lang.RDFXML);
		addData(dummyModel);
	}

	private void addData(Model dummyModel) {
		dataset.begin(ReadWrite.WRITE);
		Model model = dataset.getDefaultModel();
		try {
			if (model.isEmpty()) {
				model.add(dummyModel);
				dataset.commit();
			}
		} finally {
			dataset.end();
		}
	}

	public String queryDatabse(final String query, final String serialization) {
		ResourceRepository.LOGGER.log(Level.INFO, "Querying database...");
		Model model = ModelFactory.createDefaultModel();
		dataset.begin(ReadWrite.READ);
		try {
			QueryExecution qExec = QueryExecutionFactory.create(query, dataset);
			ResultSet rs = qExec.execSelect();
			try {
				model = rs.getResourceModel();
			} finally {
				qExec.close();
			}
		} finally {
			dataset.end();
		}

		StringWriter result = new StringWriter();
		model.write(result, serialization);

		return result.toString();
	}

	public String listResources(final String serialization) {
		ResourceRepository.LOGGER
				.log(Level.INFO, "Response to format: " + serialization);

		return this.queryDatabse(ResourceRepository.DEFAULT_SELECT, serialization);
	}

	public String listResources() {
		ResourceRepository.LOGGER
				.log(Level.INFO, "Response to default format.");
		return this
				.listResources(IResourceRepository.SERIALIZATION_RDFXML_ABBREV);
	}
}
