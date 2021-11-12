package se.raa.ksamsok.lucene;

import org.apache.xml.utils.PrefixResolver;
import org.apache.xml.utils.PrefixResolverDefault;
import org.w3c.dom.Document;

import javax.xml.namespace.NamespaceContext;
import java.util.Iterator;

/**
 * Hjälpklass för uri-prefixhantering. Används bara för DC-data fn.
 */
public class NamespaceContextImpl implements NamespaceContext {

	private final PrefixResolver resolver;

	NamespaceContextImpl(Document doc) {
		this.resolver = new PrefixResolverDefault(doc.getDocumentElement());
	}

	public String getNamespaceURI(String prefix) {
		return resolver.getNamespaceForPrefix(prefix);
	}

	public String getPrefix(String namespaceURI) {
		// EJ IMPLEMENTERAD (används dock ej i vårt fall)
		return null;
	}

	public Iterator<String> getPrefixes(String namespaceURI) {
		// EJ IMPLEMENTERAD (används dock ej i vårt fall)
		return null;
	}

}
