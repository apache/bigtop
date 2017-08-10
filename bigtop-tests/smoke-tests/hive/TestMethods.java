//Copyright [2017] [Bharat Modi]
//
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
// A masterclass containing methods which aid in the replication of access to hadoop
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class TestMethods {

	static String propertyValue(String propertyFile, String propertyName)
			throws ParserConfigurationException, SAXException, IOException,
			URISyntaxException {
		String configLocation = System.getenv("HADOOP_CONF_DIR");
		File file = new File(configLocation + "/" + propertyFile);
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
				.newInstance();
		DocumentBuilder documentBuilder = documentBuilderFactory
				.newDocumentBuilder();
		Document document = documentBuilder.parse(file);
		document.getDocumentElement().normalize();
		Element docElement = document.getDocumentElement();
		NodeList nodeList = docElement.getElementsByTagName("property");
		ArrayList<String> names = new ArrayList<String>();
		ArrayList<String> values = new ArrayList<String>();
		if (nodeList != null) {
			int length = nodeList.getLength();
			for (int i = 0; i < length; i++) {
				if (nodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
					Element element = (Element) nodeList.item(i);
					if (element.getNodeName().contains("property")) {
						names.add(element.getElementsByTagName("name").item(0)
								.getTextContent());
						values.add(element.getElementsByTagName("value")
								.item(0).getTextContent());

					}
				}
			}
		}
		String[] nameslist = names.toArray(new String[names.size()]);
		String[] valueslist = values.toArray(new String[values.size()]);
		int valuePosition = Arrays.asList(nameslist).indexOf(propertyName);
		String propertyValue = valueslist[valuePosition].toString();
		return propertyValue;
	}

	static void dropTable(Statement stmt, String newTableName)
			throws SQLException {
		stmt.executeUpdate("DROP TABLE IF EXISTS " + newTableName);
	}

	static void createTable(Statement stmt, String newTableName,
			String columnNames, String delimiter) throws SQLException {
		stmt.execute("CREATE TABLE " + newTableName + " (" + columnNames
				+ ") ROW FORMAT DELIMITED FIELDS TERMINATED BY '" + delimiter
				+ "'");
		System.out.println("Creating Table " + newTableName + "\n");
	}

	static void describeTable(Statement stmt, String newTableName)
			throws SQLException {
		ResultSet res;
		String sql = "describe " + newTableName;
		System.out.println("Running: " + sql);
		res = stmt.executeQuery(sql);
		while (res.next()) {
			System.out.println(res.getString(1) + "\t" + res.getString(2)
					+ "\t" + res.getString(3) + "\t");
		}
	}

	static void showTables(Statement stmt, String sql) throws SQLException {
		ResultSet res;
		System.out.println("Running: " + sql + "\n");
		res = stmt.executeQuery(sql);
		ResultSetMetaData rsmd = res.getMetaData();
		int columnsNumber = rsmd.getColumnCount();

		while (res.next()) {

			for (int i = 1; i <= columnsNumber; i++) {
				String columnValue = res.getString(i);
				System.out.println(columnValue);
			}

		}
		System.out.println("");
	}

	static void loadFile(String localFilepath, String HdfsURI,
			String fileDestination) throws IllegalArgumentException,
			IOException, URISyntaxException {
		Configuration conf = new Configuration();
		InputStream inputStream = new BufferedInputStream(new FileInputStream(
				localFilepath));
		FileSystem hdfs = FileSystem.get(new URI(HdfsURI), conf);
		OutputStream outputStream = hdfs.create(new Path(fileDestination));
		try {
			IOUtils.copyBytes(inputStream, outputStream, 4096, false);
		} finally {
			IOUtils.closeStream(inputStream);
			IOUtils.closeStream(outputStream);
		}

	}

	static void loadData(Statement stmt, String filePath, String newTableName)
			throws SQLException {
		String sql = "LOAD data inpath '" + filePath
				+ "' OVERWRITE into table " + newTableName;
		System.out.println("Running: " + sql + "\n");
		stmt.executeUpdate(sql);
	}

	static void deleteFile(Statement stmt, Path upload, String HdfsURI)
			throws IOException, URISyntaxException {
		Configuration conf = new Configuration();
		FileSystem hdfs = FileSystem.get(new URI(HdfsURI), conf);
		hdfs.getConf();
		if (hdfs.exists(upload)) {
			hdfs.delete(upload, true);
			System.out.println("Uploaded File has been removed");
		}
	}

	static void updateTable(Statement stmt, String selection)
			throws SQLException {
		String sql = selection;
		int affectedRows = stmt.executeUpdate(sql);
		System.out.println("Updating Table: " + sql + "\n");
		System.out.println("Affected Rows: " + affectedRows);
	}

	static String printResults(Statement stmt, String selection,
			int columnVerificationNumber) throws SQLException {
		ResultSet res;
		String sql = selection;
		res = stmt.executeQuery(sql);
		System.out.println("\n" + "Printing Results: " + sql + "\n");
		ResultSetMetaData rsmd = res.getMetaData();
		int columnsNumber = rsmd.getColumnCount();
		for (int q = 1; q <= columnsNumber; q++) {
			System.out.print(rsmd.getColumnName(q) + " ");
		}
		System.out.println("\n");
		while (res.next()) {
			for (int i = 1; i <= columnsNumber; i++) {
				String columnValue = res.getString(i);
				System.out.print(columnValue + "  ");
			}
			System.out.println("");
		}

		res = stmt.executeQuery(sql);
		return resultSetVerification(res, columnVerificationNumber);
	}

	static String resultSetVerification(ResultSet res,
			int columnVerificationNumber) throws SQLException {
		String validate = null;
		while (res.next()) {

			validate = res.getString(columnVerificationNumber);

		}
		return validate;
	}

	static void executeStatement(Statement stmt, String sqlStatement)
			throws SQLException {
		stmt.execute(sqlStatement);
	}
}
