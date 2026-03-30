package com.atu.tools.stringsync.services

import org.w3c.dom.Element
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.exists

class StringXmlParser {

    fun parse(file: Path): Map<String, String> {
        if (!file.exists()) return emptyMap()
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file.toFile())
        val nodes = document.getElementsByTagName("string")
        val map = linkedMapOf<String, String>()
        for (i in 0 until nodes.length) {
            val node = nodes.item(i) as? Element ?: continue
            val key = node.getAttribute("name")
            if (key.isNotBlank()) {
                map[key] = node.textContent ?: ""
            }
        }
        return map
    }
}
