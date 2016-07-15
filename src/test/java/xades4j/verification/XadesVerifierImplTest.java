/*
 * XAdES4j - A Java library for generation and verification of XAdES signatures.
 * Copyright (C) 2010 Luis Goncalves.
 *
 * XAdES4j is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or any later version.
 *
 * XAdES4j is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License along
 * with XAdES4j. If not, see <http://www.gnu.org/licenses/>.
 */
package xades4j.verification;

import java.io.FileInputStream;
import java.io.InputStream;
import org.apache.xml.security.utils.resolver.ResourceResolver;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import xades4j.production.TestResolverSpi;
import xades4j.production.XadesFormatExtenderProfile;
import xades4j.production.XadesSignatureFormatExtender;
import xades4j.properties.ArchiveTimeStampProperty;
import xades4j.properties.AttrAuthoritiesCertValuesProperty;
import xades4j.properties.AttributeRevocationValuesProperty;
import xades4j.properties.CertificateValuesProperty;
import xades4j.properties.QualifyingProperty;
import xades4j.properties.RevocationValuesProperty;
import xades4j.properties.SigAndRefsTimeStampProperty;
import static org.junit.Assume.assumeTrue;

/**
 *
 * @author Luís
 */
public class XadesVerifierImplTest extends VerifierTestBase
{
    XadesVerificationProfile verificationProfile;
    XadesVerificationProfile nistVerificationProfile;

    @Before
    public void initialize()
    {
        verificationProfile = new XadesVerificationProfile(
                                    VerifierTestBase.validationProviderMySigs,
                                    VerifierTestBase.tsaValidationProviderMySigs);
        nistVerificationProfile = new XadesVerificationProfile(
                                    VerifierTestBase.validationProviderNist,
                                    VerifierTestBase.tsaValidationProviderNist);
    }

    @Test
    public void testVerifyBES() throws Exception
    {
        System.out.println("verifyBES");
        XAdESForm f = verifySignature("document.signed.bes.xml");
        assertEquals(XAdESForm.BES, f);
    }

    @Test(expected = InvalidSignatureException.class)
    public void testVerifyWithCustomRawVerifier() throws Exception
    {
        System.out.println("verifyWithCustomRawVerifier");
        verificationProfile.withRawSignatureVerifier(new RawSignatureVerifier()
        {
            @Override
            public void verify(RawSignatureVerifierContext ctx) throws InvalidSignatureException
            {
                // Do something usefull with the signature
                // ctx.getSignature().getSignedInfo().item(0)...
                throw new InvalidSignatureException("Rejected by RawSignatureVerifier");
            }
        });
        XAdESForm f = verifySignature("document.signed.bes.xml", verificationProfile);
        assertEquals(XAdESForm.BES, f);
    }

    @Test
    public void testVerifyDetachedBES() throws Exception
    {
        System.out.println("verifyDetachedBES");

        Document doc = getDocument("detached.bes.xml");
        Element signatureNode = getSigElement(doc);

        SignatureSpecificVerificationOptions options = new SignatureSpecificVerificationOptions().useResourceResolver(new ResourceResolver(new TestResolverSpi()));

        XAdESVerificationResult res = verificationProfile.newVerifier().verify(signatureNode, options);
        assertEquals(XAdESForm.BES, res.getSignatureForm());
    }
    
    @Test
    public void testVerifyBESCounterSig() throws Exception
    {
        System.out.println("verifyBESCounterSig");
        XAdESForm f = verifySignature("document.signed.bes.cs.xml");
        assertEquals(XAdESForm.BES, f);
    }

    @Test
    public void testVerifyBESEnrichT() throws Exception
    {
        System.out.println("verifyBESEnrichT");

        Document doc = getDocument("document.signed.bes.xml");
        Element signatureNode = getSigElement(doc);

        XadesSignatureFormatExtender formExt = new XadesFormatExtenderProfile().getFormatExtender();
        XAdESVerificationResult res = verificationProfile.newVerifier().verify(signatureNode, null, formExt, XAdESForm.T);
        assertEquals(XAdESForm.BES, res.getSignatureForm());

        res = verificationProfile.newVerifier().verify(signatureNode, null);
        assertEquals(XAdESForm.T, res.getSignatureForm());

        outputDocument(doc, "document.verified.bes.t.xml");
    }

    @Test
    public void testVerifyBESExtrnlResEnrichC() throws Exception
    {
        System.out.println("verifyBESExtrnlResEnrichC");

        Document doc = getDocument("document.signed.bes.extres.xml");
        Element signatureNode = getSigElement(doc);
        SignatureSpecificVerificationOptions options = new SignatureSpecificVerificationOptions().useBaseUri("http://www.ietf.org/rfc/");

        XadesSignatureFormatExtender formExt = new XadesFormatExtenderProfile().getFormatExtender();

        XAdESVerificationResult res = nistVerificationProfile.newVerifier().verify(signatureNode, options, formExt, XAdESForm.C);
        assertEquals(XAdESForm.BES, res.getSignatureForm());

        res = nistVerificationProfile.newVerifier().verify(signatureNode, options);
        assertEquals(XAdESForm.C, res.getSignatureForm());

        outputDocument(doc, "document.verified.bes.extres.c.xml");
    }

    @Test
    public void testVerifyTBES() throws Exception
    {
        System.out.println("verifyTBES");
        XAdESForm f = verifySignature("document.signed.t.bes.xml");
        assertEquals(XAdESForm.T, f);
    }

    @Test
    public void testVerifyEPES() throws Exception
    {
        System.out.println("verifyEPES");
        verificationProfile.withPolicyDocumentProvider(VerifierTestBase.policyDocumentFinder);
        XAdESForm f = verifySignature("document.signed.epes.xml", verificationProfile);
        assertEquals(XAdESForm.EPES, f);
    }

    @Test
    public void testVerifyTEPES() throws Exception
    {
        System.out.println("verifyTEPES");
        XAdESForm f = verifySignature("document.signed.t.epes.xml");
        assertEquals(XAdESForm.T, f);
    }

    /**
     * XXX fails because we do not have a signed document with valid ptcc certificate
     *
     * @throws Exception
     */
    @Test
    @Ignore
    public void testVerifyTPTCC() throws Exception    {
        System.out.println("verifyTPtCC");
        assumeTrue(onWindowsPlatform() && null != validationProviderPtCc);

        XAdESForm f = verifySignature("document.signed.t.bes.ptcc.xml",
                new XadesVerificationProfile(validationProviderPtCc,
                        tsaValidationProviderMySigs));
        assertEquals(XAdESForm.T, f);
    }

    @Test
    public void testVerifyC() throws Exception
    {
        System.out.println("verifyC");
        XAdESForm f = verifySignature(
                "document.signed.c.xml",
                nistVerificationProfile);
        assertEquals(XAdESForm.C, f);
    }

    @Test
    public void testVerifyDetachedC() throws Exception
    {
        System.out.println("verifyDetachedC");

        Document doc = getDocument("detached.c.xml");
        Element signatureNode = getSigElement(doc);
        XadesVerifier verifier = nistVerificationProfile.newVerifier();

        InputStream is = new FileInputStream("license.txt");
        SignatureSpecificVerificationOptions options = new SignatureSpecificVerificationOptions().useDataForAnonymousReference(is);
        XAdESVerificationResult res = verifier.verify(signatureNode, options);
        
        // The caller must close the stream.
        is.close();

        assertEquals(XAdESForm.C, res.getSignatureForm());
    }

    @Test
    public void testVerifyCEnrichX() throws Exception
    {
        System.out.println("verifyCEnrichX");

        Document doc = getDocument("document.signed.c.xml");
        Element signatureNode = getSigElement(doc);

        XadesSignatureFormatExtender formExt = new XadesFormatExtenderProfile().getFormatExtender();
        XAdESVerificationResult res = nistVerificationProfile.newVerifier().verify(signatureNode, null, formExt, XAdESForm.X);

        assertEquals(XAdESForm.C, res.getSignatureForm());
        assertPropElementPresent(signatureNode, SigAndRefsTimeStampProperty.PROP_NAME);

        outputDocument(doc, "document.verified.c.x.xml");

    }

    @Test
    public void testVerifyCEnrichXL() throws Exception
    {
        System.out.println("verifyCEnrichXL");

        Document doc = getDocument("document.signed.c.xml");
        Element signatureNode = getSigElement(doc);

        XadesSignatureFormatExtender formExt = new XadesFormatExtenderProfile().getFormatExtender();
        XAdESVerificationResult res = nistVerificationProfile.newVerifier().verify(signatureNode, null, formExt, XAdESForm.X_L);

        assertEquals(XAdESForm.C, res.getSignatureForm());
        assertPropElementPresent(signatureNode, SigAndRefsTimeStampProperty.PROP_NAME);
        assertPropElementPresent(signatureNode, CertificateValuesProperty.PROP_NAME);
        assertPropElementPresent(signatureNode, RevocationValuesProperty.PROP_NAME);
        assertPropElementPresent(signatureNode, AttrAuthoritiesCertValuesProperty.PROP_NAME);
        assertPropElementPresent(signatureNode, AttributeRevocationValuesProperty.PROP_NAME);

        outputDocument(doc, "document.verified.c.xl.xml");
    }

    @Test
    public void testVerifyX() throws Exception
    {
        System.out.println("verifyX");
        XAdESForm f = verifySignature(
                "document.verified.c.x.xml",
                nistVerificationProfile);
        assertEquals(XAdESForm.X, f);
    }

    @Test
    public void testVerifyXL() throws Exception
    {
        System.out.println("verifyXL");
        XAdESForm f = verifySignature(
                "document.verified.c.xl.xml",
                nistVerificationProfile);
        assertEquals(XAdESForm.X_L, f);
    }

    @Test
    public void testVerifyXLEnrichA() throws Exception
    {
        System.out.println("testVerifyXLEnrichA");

        Document doc = getDocument("document.verified.c.xl.xml");
        Element signatureNode = getSigElement(doc);

        XadesSignatureFormatExtender formExt =
                new XadesFormatExtenderProfile().getFormatExtender();
        XAdESVerificationResult res = nistVerificationProfile.newVerifier().verify(
                signatureNode, null, formExt, XAdESForm.A);

        assertEquals(XAdESForm.X_L, res.getSignatureForm());
        assertPropXAdES141ElementPresent(signatureNode, ArchiveTimeStampProperty.PROP_NAME);

        outputDocument(doc, "document.verified.c.xl.a.xml");
    }

    @Test
    public void testVerifyA() throws Exception
    {
        System.out.println("testVerifyA");
        XAdESForm f = verifySignature(
                "document.verified.c.xl.a.xml",
                nistVerificationProfile);
        assertEquals(XAdESForm.A, f);
    }

    @Test
    public void testVerifyAEnrichAVD() throws Exception
    {
        System.out.println("testVerifyXLEnrichA");

        Document doc = getDocument("document.verified.c.xl.a.xml");
        Element signatureNode = getSigElement(doc);

        XadesSignatureFormatExtender formExt =
                new XadesFormatExtenderProfile().getFormatExtender();
        XAdESVerificationResult res = nistVerificationProfile.newVerifier().verify(
                signatureNode, null, formExt, XAdESForm.A_VD);

        assertEquals(XAdESForm.A, res.getSignatureForm());
        // TODO recent changes to verifier made the property generated only when needed,
        // as all information that would be stored in it is already present in other
        // properties, the property won't be created
        //assertPropXAdES141ElementPresent(signatureNode, TimeStampValidationDataProperty.PROP_NAME);

        outputDocument(doc, "document.verified.c.xl.avd.xml");
    }


    private static void assertPropElementPresent(
            Element sigElem,
            String elemName)
    {
        NodeList props = sigElem.getElementsByTagNameNS(QualifyingProperty.XADES_XMLNS, elemName);
        assertFalse(props.getLength() == 0);
    }

    private static void assertPropXAdES141ElementPresent(
            Element sigElem,
            String elemName)
    {
        NodeList props = sigElem.getElementsByTagNameNS(
                QualifyingProperty.XADESV141_XMLNS, elemName);
        assertFalse(props.getLength() == 0);
    }
}
