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
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.tdb.TDBFactory;

public class OLDResourceRepository implements IResourceRepository {

	private static final String DEFAULT_SELECT = "SELECT * {?s ?p ?o} LIMIT 100";
	private static final String REPODB_DIR = "repodb";
	private final static Logger LOGGER = Logger
			.getLogger(OLDResourceRepository.class.toString());
	private Dataset dataset;

	public OLDResourceRepository() {
		this.dataset = TDBFactory.createDataset(OLDResourceRepository.REPODB_DIR);
	}

	public OLDResourceRepository(String filename) {
		this();
		addDummyData(filename);
	}

	private void addDummyData(String filename) {
		OLDResourceRepository.LOGGER.log(Level.INFO, "Loading data from: "
				+ filename);
		Model dataModel = RDFDataMgr.loadModel(filename, Lang.RDFXML);
		addData(dataModel);
	}

	private void addData(Model dataModel) {
		dataset.begin(ReadWrite.WRITE);
		Model defaultModel = dataset.getDefaultModel();
		try {
			//if (defaultModel.isEmpty()) {
				defaultModel.add(dataModel);
				dataset.commit();
			//}
		} finally {
			dataset.end();
		}
	}

	public String queryDatabase(final String query) {
		return queryDatabse(query,
				IResourceRepository.SERIALIZATION_RDFXML_ABBREV);
	}

	public String queryDatabse(final String query, final String serialization) {
		OLDResourceRepository.LOGGER.log(Level.INFO, "Querying database '" + query
				+ "'...");
		Model model = ModelFactory.createDefaultModel();

		dataset.begin(ReadWrite.READ);
		try {
			QueryExecution qExec = QueryExecutionFactory.create(query, dataset);

			try {
				if (query.toLowerCase().startsWith("select")) {
					ResultSet rs = qExec.execSelect();
					model = ResultSetFormatter.toModel(rs);
				} else if (query.toLowerCase().startsWith("construct")) {
					model = qExec.execConstruct();
				} else if (query.toLowerCase().startsWith("describe")) {
					model = qExec.execDescribe();
				}
				// model = rs.getResourceModel();
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
		OLDResourceRepository.LOGGER.log(Level.INFO, "Response to format: "
				+ serialization);

		return this.queryDatabse(OLDResourceRepository.DEFAULT_SELECT,
				serialization);
	}

	public String listResources() {
		OLDResourceRepository.LOGGER
				.log(Level.INFO, "Response to default format.");
		return this
				.listResources(IResourceRepository.SERIALIZATION_RDFXML_ABBREV);
	}
}
