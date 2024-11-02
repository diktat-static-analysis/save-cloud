package com.saveourtool.common.demo.cpg

/**
 * Engines for CPG demo
 *
 * @property prettyName
 */
enum class CpgEngine(val prettyName: String) {
    /**
     * A default engine using [Fraunhofer-AISEC/cpg](https://github.com/Fraunhofer-AISEC/cpg)
     */
    CPG("cpg (Fraunhofer-AISEC)"),

    /**
     * A tree-sitter engine using [kotlintree](https://github.com/oxisto/kotlintree) as a binding
     */
    TREE_SITTER("tree-sitter (kotlintree)"),
    ;
}
