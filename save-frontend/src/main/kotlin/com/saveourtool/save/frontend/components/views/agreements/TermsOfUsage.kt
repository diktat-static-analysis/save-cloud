/**
 * File containing Terms of Use view
 */

package com.saveourtool.save.frontend.components.views.agreements

import com.saveourtool.frontend.common.components.basic.markdown
import com.saveourtool.frontend.common.utils.Style
import com.saveourtool.frontend.common.utils.useBackground
import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import web.cssom.ClassName

private val acceptanceOfTerms = "## 1. Acceptance of Terms" to """
    |1.1. Agreement: This Agreement constitutes a legally binding contract between you (referred to as "User,"
    |"You," or "Your") and SaveOurTool (referred to as "we," "us," or "our"), governing your use of
    |www.saveourtool.com and any associated services.
    |
    |1.2. Modifications: We reserve the right to modify or update this Agreement at any time.
    |It is your responsibility to review the Agreement periodically.
    |Your continued use of our website after any modifications implies your acceptance of the revised terms.
""".trimMargin()

private val userAccounts = "## 2. User Accounts" to """
    |2.1 Account Creation: To access certain features or services, you may need to create a user account.
    |By creating an account, you agree to provide accurate, current, and complete information.
    |You are solely responsible for maintaining the confidentiality of your account credentials.
    |   
    |2.2 Third-Party Authentication: Our website allows you to sign in using third-party services
    |(such as Google, Github, Gitee, Huawei, Apple). You understand and agree that you are solely responsible
    |for complying with the terms and conditions of those services, and we are not responsible for any actions
    |or issues arising from your use of such authentication services.
""".trimMargin()

private val intellectualProperty = "## 3. Intellectual Property" to """
    |3.1 Ownership: All content, materials, and features available on our website are protected by intellectual
    |property rights and are the property of SaveOurTool or our licensors.
    |You may not use, reproduce, distribute, or modify any content without obtaining prior written permission from us.
    |
    |3.2 User Submissions: By submitting any content (including files) to our website, you grant us a non-exclusive,
    |worldwide, royalty-free license to use, reproduce, modify, adapt, publish, translate, distribute,
    |and display such content for the purpose of providing and promoting our services.
    |You represent and warrant that you have all necessary rights to grant this license.
""".trimMargin()

private val prohibitedConduct = "## 4. Prohibited Conduct" to """
    |4.1 Compliance: You agree to use our website in compliance with all applicable laws, regulations,
    |and this Agreement. You are prohibited from engaging in any activity that may:
    |
    |    a. Infringe upon or violate the rights of others;
    |
    |    b. Impersonate any person or entity or falsely state or misrepresent your affiliation with a person or entity;
    |
    |    c. Introduce malicious software, viruses, or other harmful content;
    |
    |    d. Interfere with or disrupt the integrity or performance of our website or its underlying infrastructure;
    |
    |    e. Harvest or collect user data without permission;
    |
    |    f. Engage in any unauthorized advertising or promotional activities.
""".trimMargin()

private val dataPrivacy = "## 5. Data Privacy and Security" to """
    |5.1 Data Collection: By using our website, you agree that we may collect and process certain information about you.
    |Please refer to our Privacy Policy for more details on how we handle your personal information.
    |
    |5.2 User Content: While we take reasonable measures to protect user data, we cannot guarantee the security of
    |user-submitted content or files. You acknowledge and accept the risks associated with uploading files to our website.
""".trimMargin()

private val limitationsOfLiability = "## 6. Limitation of Liability" to """
    |6.1 Disclaimer: Our website and services are provided on an "as is" and "as available" basis,
    |without warranties of any kind, either express or implied.
    |We do not guarantee the accuracy, completeness, or reliability of any information on our website.
    |
    |6.2 Indemnification: You agree to indemnify and hold us harmless from any claims, damages,
    |liabilities, or expenses arising out of your use of our website, your violation of this Agreement,
    |or your infringement of any third-party rights.
""".trimMargin()

private val termination = "## 7. Termination" to """
    |7.1 Termination: We reserve the right to terminate or suspend your access to our website, without prior notice,
    |for any reason or no reason, at our sole discretion.
    |
    |7.2 Survival: The provisions regarding intellectual property, prohibited conduct, data privacy,
    |limitation of liability, and indemnification shall survive any termination or expiration of this Agreement.
""".trimMargin()

private val lawAndDisputes = "## 8. Governing Law and Dispute Resolution" to """
    |8.1 Governing Law: This Agreement shall be governed by and construed in accordance with the laws of [jurisdiction],
    |without regard to its conflict of laws principles.
    |
    |8.2 Dispute Resolution: Any dispute arising out of or relating to this Agreement shall be resolved through
    |arbitration in accordance with the rules of [arbitration institution].
    |The arbitration shall take place in [city, country], and the language of the arbitration shall be [language].
    |Each party shall bear its own costs and expenses related to the arbitration.
""".trimMargin()

private val generalProvisions = "## 9. General Provisions" to """
    |9.1 Entire Agreement: This Agreement constitutes the entire agreement between you and SaveOurTool regarding the
    |subject matter hereof and supersedes all prior or contemporaneous communications, understandings, and agreements,
    |whether oral or written.
    |
    |9.2 Severability: If any provision of this Agreement is found to be invalid, illegal, or unenforceable,
    |the remaining provisions shall continue in full force and effect.
""".trimMargin()

private val termsOfUseGeneral = """
    |# Terms of Use Agreement
    |
""".trimMargin() to """
    |Welcome to www.saveourtool.com!
    |
    |Please carefully read these Terms of Use ("Agreement") before using our website.
    |By accessing or using our services, you agree to be bound by this Agreement.
    |If you do not agree to these terms, please refrain from using our website.
""".trimMargin()

private val termsOfUsageContent = listOf(
    termsOfUseGeneral,
    acceptanceOfTerms,
    userAccounts,
    intellectualProperty,
    prohibitedConduct,
    dataPrivacy,
    limitationsOfLiability,
    termination,
    lawAndDisputes,
    generalProvisions,
)

val termsOfUsageView: FC<Props> = FC {
    useBackground(Style.INDEX)

    div {
        className = ClassName("mx-auto card card-body border col-5 my-5")

        termsOfUsageContent.forEach { (title, content) ->
            div {
                markdown(title)
                markdown(content)
            }
        }
    }
}
