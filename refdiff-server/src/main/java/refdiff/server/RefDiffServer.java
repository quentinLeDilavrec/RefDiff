package refdiff.server;

import java.io.File;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import refdiff.core.RefDiff;
import refdiff.core.cst.CstNode;
import refdiff.core.diff.CstComparator;
import refdiff.core.diff.CstDiff;
import refdiff.core.diff.Relationship;
import refdiff.core.util.PairBeforeAfter;
import refdiff.parsers.c.CPlugin;
import refdiff.parsers.java.JavaPlugin;
import refdiff.core.io.SourceFileSet;
import refdiff.core.cst.Location;
// import refdiff.parsers.js.JsPlugin;

import static spark.Spark.*;

public class RefDiffServer {

	public static void main(String[] args) throws Exception {
		System.out.println("launching server");
		Set<String> validOrigins = new HashSet<String>();
		validOrigins.add("http://127.0.0.1:8080");
		validOrigins.add("http://localhost:8080");
		validOrigins.add("http://127.0.0.1:8081");
		validOrigins.add("http://localhost:8081");
		validOrigins.add("http://131.254.17.96:8080");
		port(8089);
		options("/RefDiff", (req, res) -> {
			System.out.println("====options=====RefDiff=========");
			System.out.println(req.headers("Origin"));
			res.header("Content-Type", "application/json");
			if (validOrigins.contains(req.headers("Origin"))) {
				res.header("Access-Control-Allow-Origin", req.headers("Origin"));
			}
			res.header("Access-Control-Allow-Credentials", "true");
			res.header("Access-Control-Allow-Methods", "OPTIONS,PUT,GET");
			res.header("Access-Control-Allow-Headers",
					"Origin, Access-Control-Allow-Origin, Access-Control-Allow-Credentials");
			return "";
		});
		// runExamples();
		File tempFolder = new File("temp");
		File rawsTempFolder = new File(tempFolder, "raw");
		put("/RefDiff", (req, res) -> {
			File currTempFolder = new File(rawsTempFolder,
					new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date()));
			System.out.println("=========RefDiff=========");
			System.out.println(req.headers("Origin"));
			res.header("Content-Type", "application/json");
			if (validOrigins.contains(req.headers("Origin"))) {
				res.header("Access-Control-Allow-Origin", req.headers("Origin"));
			}
			res.header("Access-Control-Allow-Credentials", "true");
			res.header("Access-Control-Allow-Methods", "OPTIONS,PUT,GET");

			String[] vals = req.body().split("\n", 2);

			Map<String, String> files1 = new HashMap<String, String>();
			files1.put("p1/A.java", new String(Base64.getDecoder().decode(vals[0])));
			Map<String, String> files2 = new HashMap<String, String>();
			files2.put("p1/A.java", new String(Base64.getDecoder().decode(vals[1])));

			// System.out.println(new String(Base64.getDecoder().decode(vals[0])));
			// System.out.println("Base64.getDecoder().decode(vals[0])");
			// System.out.println(new String(Base64.getDecoder().decode(vals[1])));

			JavaPlugin javaPlugin = new JavaPlugin(tempFolder);
			CstComparator comparator = new CstComparator(javaPlugin);
			File neww = new File(currTempFolder, "new");
			File old = new File(currTempFolder, "old");
			PairBeforeAfter<SourceFileSet> beforeAndAfter = new PairBeforeAfter<SourceFileSet>(
					new VirtSource(neww, files1), new VirtSource(old, files2));
			beforeAndAfter.getBefore().materializeAtBase(neww.toPath());
			beforeAndAfter.getAfter().materializeAtBase(old.toPath());
			CstDiff diff = comparator.compare(beforeAndAfter);
			// JsonElement r = refactorings2json("Refactorings found in given files", diff);
			// Gson gson = new GsonBuilder().setPrettyPrinting().create();
			// currTempFolder.delete();
			// return gson.toJson(r);
			SimpleModule module = new SimpleModule();
			ObjectMapper mapper = new ObjectMapper();
			try {
				return mapper.registerModule(module)
				.writer(new DefaultPrettyPrinter())
				.writeValueAsString(diff);
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return "";
		});
		exception(Exception.class, (exception, request, response) -> {
			// Handle the exception here
			System.out.println("Exception");
			System.out.println(exception.getMessage());
			System.out.println(exception.getStackTrace());
			StackTraceElement[] l = exception.getStackTrace();
			for (int i = 0; i < l.length; i++) {
				System.out.println(l[i]);
			}
			System.out.println(request);
			System.out.println(response);
			response.status(400);
		});

	}

	private static void runExamples() throws Exception {
		// This is a temp folder to clone or checkout git repositories.
		File tempFolder = new File("temp");

		// // Creates a RefDiff instance configured with the JavaScript plugin.
		// JsPlugin jsPlugin = new JsPlugin();
		// RefDiff refDiffJs = new RefDiff(jsPlugin);

		// // Clone the angular.js GitHub repo.
		// File angularJsRepo = refDiffJs.cloneGitRepository(
		// new File(tempFolder, "angular.js"),
		// "https://github.com/refdiff-study/angular.js.git");

		// // You can compute the relationships between the code elements in a commit
		// with
		// // its previous commit. The result of this operation is a CstDiff object,
		// which
		// // contains all relationships between CstNodes. Relationships whose type is
		// different
		// // from RelationshipType.SAME are refactorings.
		// CstDiff diffForCommit = refDiffJs.computeDiffForCommit(angularJsRepo,
		// "2636105");
		// printRefactorings("Refactorings found in angular.js 2636105", diffForCommit);

		// // You can also mine refactoring from the commit history. In this example we
		// navigate
		// // the commit graph backwards up to 5 commits. Merge commits are skipped.
		// refDiffJs.computeDiffForCommitHistory(angularJsRepo, 5, (commit, diff) -> {
		// printRefactorings("Refactorings found in angular.js " +
		// commit.getId().name(), diff);
		// });

		// // The JsPlugin initializes JavaScript runtime to run the Babel parser. We
		// should close it shut down.
		// jsPlugin.close();

		// In this example, we use the plugin for C.
		CPlugin cPlugin = new CPlugin();
		RefDiff refDiffC = new RefDiff(cPlugin);

		File gitRepo = refDiffC.cloneGitRepository(new File(tempFolder, "git"),
				"https://github.com/refdiff-study/git.git");

		printRefactorings("Refactorings found in git ba97aea",
				refDiffC.computeDiffForCommit(gitRepo, "ba97aea1659e249a3a58ecc5f583ee2056a90ad8"));

		// Now, we use the plugin for Java.
		JavaPlugin javaPlugin = new JavaPlugin(tempFolder);
		RefDiff refDiffJava = new RefDiff(javaPlugin);

		File eclipseThemesRepo = refDiffJava.cloneGitRepository(new File(tempFolder, "eclipse-themes"),
				"https://github.com/icse18-refactorings/eclipse-themes.git");

		printRefactorings("Refactorings found in eclipse-themes 72f61ec",
				refDiffJava.computeDiffForCommit(eclipseThemesRepo, "72f61ec"));
	}

	private static void printRefactorings(String headLine, CstDiff diff) {
		System.out.println(headLine);
		for (Relationship rel : diff.getRefactoringRelationships()) {
			System.out.println(rel.getStandardDescription());
		}
	}

	private static JsonElement refactorings2json(String headLine, CstDiff diff) {
		JsonArray r = new JsonArray();

		System.out.println(headLine);
		// System.out.println(diff.getRelationships().size());
		for (Relationship rel : diff.getRelationships()) {
			JsonObject o = new JsonObject();
			r.add(o);
			o.addProperty("type", rel.getType().name());
			o.addProperty("fromP", formatWithLineNum(rel.getNodeBefore()));
			o.addProperty("toP", formatWithLineNum(rel.getNodeAfter()));
			o.add("from", node2json(rel.getNodeBefore(), "left"));
			o.add("to", node2json(rel.getNodeAfter(), "right"));
			o.addProperty("fromJ", node2jsonBis(rel.getNodeBefore(), "left"));
			o.addProperty("toJ", node2jsonBis(rel.getNodeAfter(), "right"));
			// o.addProperty("similarity",rel.getSimilarity().toString());
			o.addProperty("value", rel.getStandardDescription());
		}
		return r;
	}

	private static JsonObject node2json(CstNode node, String side) {
		JsonObject o = node2json(node);
		o.addProperty("side", side);
		return o;
	}

	private static String node2jsonBis(CstNode node, String side) {
		SimpleModule module = new SimpleModule();
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.registerModule(module).writer(new DefaultPrettyPrinter()).writeValueAsString(node);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}
	
	private static JsonObject node2json(CstNode node) {
		JsonObject o = new JsonObject();
		o.addProperty("type",node.getType());
		o.addProperty("name",node.getLocalName());
		o.addProperty("start",node.getLocation().getBegin());
		o.addProperty("end",node.getLocation().getEnd());
		o.addProperty("bstart",node.getLocation().getBodyBegin());
		o.addProperty("bend",node.getLocation().getBodyEnd());
		o.addProperty("file",node.getLocation().getFile());
		o.add("loc",loc2json(node.getLocation()));
		return o;
		// String.format("%s %s at %s:%d", 
		// node.getType().replace("Declaration", ""), 
		// node.getLocalName(), 
		// node.getLocation().getFile(), 
		// node.getLocation().getLine());
	}
	
	private static JsonObject loc2json(Location loc) {
		JsonObject o = new JsonObject();
		JsonObject start = new JsonObject();
		o.add("start", start);
		start.addProperty("line",loc.getLine());
		return o;
	}
	
	private static String formatWithLineNum(CstNode node) {
		return String.format("%s %s at %s:%d", node.getType().replace("Declaration", ""), node.getLocalName(), node.getLocation().getFile(), node.getLocation().getLine());
	}
}
