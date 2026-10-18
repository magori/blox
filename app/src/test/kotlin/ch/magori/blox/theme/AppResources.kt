package ch.magori.blox.theme

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

/** The XML resource at [path] in the res directory of the application, which the build gives in `blox.resDir`. */
internal fun appResource(path: String): Document {
    val file = File(buildProperty("blox.resDir", "the res directory of the application"), path)
    check(file.isFile) { "The application has no resource $path" }
    return parse(file)
}

/** The manifest of the application, which the build gives in `blox.manifest`. */
internal fun appManifest(): Document = parse(File(buildProperty("blox.manifest", "the manifest of the application")))

private fun buildProperty(
    name: String,
    what: String,
): String = checkNotNull(System.getProperty(name)) { "The build gives $what in the system property $name" }

private fun parse(file: File): Document =
    DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }.newDocumentBuilder().parse(file)

/** The elements of [document] that [expression] finds, in document order. */
internal fun elements(
    document: Document,
    expression: String,
): List<Element> {
    val nodes = XPathFactory.newInstance().newXPath().evaluate(expression, document, XPathConstants.NODESET) as NodeList
    return (0 until nodes.length).map { nodes.item(it) as Element }
}

/** The single element of [document] that [expression] finds, or a failure that says which one is missing. */
internal fun element(
    document: Document,
    expression: String,
): Element = checkNotNull(elements(document, expression).singleOrNull()) { "No single element for $expression" }

/** The Android namespace, for the `android:` attributes of a resource. */
internal const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
