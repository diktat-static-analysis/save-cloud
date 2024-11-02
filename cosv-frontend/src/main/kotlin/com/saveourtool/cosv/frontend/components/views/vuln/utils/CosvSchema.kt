/**
 * The base spec of COSV schema
 */

package com.saveourtool.cosv.frontend.components.views.vuln.utils

const val COSV_SCHEMA_JSON = """
{
  "schema_version": "string",
  "id": "string",
  "modified": "string",
  "published": "string",
  "withdrawn": "string",
  "aliases": [ "string" ],
  "cwe_ids": [ "string" ],
  "cwe_names": [ "string" ],
  "timeline": [ {
      "timeline.type": "string",
      "timeline.value": "string"
    }
  ],
  "related": [ "string" ],
  "summary": "string",
  "details": "string",
  "severity": [ {
      "severity.type": "string",
      "severity.score": "string",
      "severity.level": "string",
      "severity.score_num": "string"
    }
  ],
  "affected": [ {
      "package": {
        "ecosystem": "string",
        "package.name": "string",
        "purl": "string",
        "language": "string",
        "repository": "string",
        "introduced_commits": [ "string" ],
        "fixed_commits": [ "string" ],
        "home_page": "string",
        "edition": "string"
      },
      "severity": [ {
          "severity.type": "string",
          "severity.score": "string",
          "severity.level": "string",
          "severity.score_num": "string"
        }
      ],
      "ranges": [ {
          "ranges.type": "string",
          "ranges.repo": "string",
          "events": [ {
              "introduced": "string",
              "fixed": "string",
              "last_affected": "string",
              "limit": "string"
            }
          ],
          "ranges.database_specific": { Object }
        }
      ],
      "versions": [ "string" ],
      "affected.ecosystem_specific": { Object },
      "affected.database_specific": { Object }
    }
  ],
  "patches_detail": [ {
      "patch_url": "string",
      "issue_url": "string",
      "main_language": "string",
      "author": "string",
      "commiter": "string",
      "branches": [ "string" ],
      "tags": [ "string" ]
    }
  ],
  "contributors": [ {
      "org": "string",
      "contributors.name": "string",
      "email": "string",
      "contributions": "string"
    }
  ],
  "confirm_type": "string",
  "references": [ {
      "references.type": "string",
      "url": "string"
    }
  ],
  "credits": [ {
      "credits.name": "string",
      "contact": [ "string" ],
      "credits.type": "string"
    }
  ],
  "database_specific": { Object }
}
"""
