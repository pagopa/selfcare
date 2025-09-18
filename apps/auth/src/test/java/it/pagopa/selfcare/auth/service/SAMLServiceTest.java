package it.pagopa.selfcare.auth.service;


import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.exception.SamlSignatureException;
import it.pagopa.selfcare.auth.util.SamlValidator;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
public class SAMLServiceTest {
  @InjectMock
  SamlValidator samlValidator;

  @Inject
  SAMLService samlService;

  private static final String TEST_SAML = """
    <?xml version="1.0" encoding="UTF-8" standalone="no"?>
                      <saml2p:Response
                      	xmlns:saml2p="urn:oasis:names:tc:SAML:2.0:protocol" Destination="https://dev.selfcare.test.it/saml/acs" ID="_123456789" IssueInstant="2025-09-05T11:01:16.063Z" Version="2.0">
                      	<saml2:Issuer
                      		xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion">https://accounts.test.com/o/saml2?idpid=123456
                      	</saml2:Issuer>
                      	<saml2p:Status>
                      		<saml2p:StatusCode Value="urn:oasis:names:tc:SAML:2.0:status:Success"/>
                      	</saml2p:Status>
                      	<saml2:Assertion
                      		xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion" ID="_9e1fa8afb432c3115dbc9c47848d4555" IssueInstant="2025-09-05T11:01:16.063Z" Version="2.0">
                      		<saml2:Issuer>https://accounts.test.com/o/saml2?idpid=123456</saml2:Issuer>
                      		<ds:Signature
                      			xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
                      			<ds:SignedInfo>
                      				<ds:CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/>
                      				<ds:SignatureMethod Algorithm="http://www.w3.org/2001/04/xmldsig-more#rsa-sha256"/>
                      				<ds:Reference URI="#_9e1fa8afb432c3115dbc9c47848d4555">
                      					<ds:Transforms>
                      						<ds:Transform Algorithm="http://www.w3.org/2000/09/xmldsig#enveloped-signature"/>
                      						<ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/>
                      					</ds:Transforms>
                      					<ds:DigestMethod Algorithm="http://www.w3.org/2001/04/xmlenc#sha256"/>
                      					<ds:DigestValue>VVDuF3rISQXSRlHyR/w7fuzI8p2pU3SnF57+sW3y0bU=</ds:DigestValue>
                      				</ds:Reference>
                      			</ds:SignedInfo>
                      			<ds:SignatureValue>bUfZkG4iX2l+bUYXyd3wUAafM8l0njf09pPVTGljgTh5ZbrB6sQMb6MU7DW5Rt5M1vYrI0fM8EOF
                      VVT+iC8Y3JqJvtQ8KEmGs8tq+fWOWcx90wfcW9FA9TukdLcyUwHHTzSv37XWpBEHoSwnAx9tz6x4
                      XSGzZ/ihYvcWplTOOdjaWJypPNHv+em/2hgSywXJ2SFCu6qyswU5X/yhFKQUlSNzCZM/KweK6Mco
                      YM45FCR7o7pYFyN+FRGA77o4KFTqdYgmHW+I4YFTdbYyL1kTcshJBErwgrLRwHhUtpYerigcg24S
                      tY3qX82WniSWMlnO4jw27GEpjbHZePAra48Njw==</ds:SignatureValue>
                      			<ds:KeyInfo>
                      				<ds:X509Data>
                      					<ds:X509SubjectName>ST=California,C=US,OU=Test Workspace,CN=Test,L=Rome,O=Test S.p.A.</ds:X509SubjectName>
                      					<ds:X509Certificate>MIIDdjCCAl6gAwIBAgIGAZRLqHfKMA0GCSqGSIb3DQEBCwUAMHwxFDASBgNVBAoTC0dvb2dsZSBJ
                      bmMuMRYwFAYDVQQHEw1Nb3VudGFpbiBWaWV3MQ8wDQYDVQQDEwZHb29nbGUxGTAXBgNVBAsTEEdv
                      b2dsZSBXb3Jrc3BhY2UxCzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpDYWxpZm9ybmlhMB4XDTI1MDEw
                      OTE1MjE1OFoXDTMwMDEwODE1MjE1OFowfDEUMBIGA1UEChMLR29vZ2xlIEluYy4xFjAUBgNVBAcT
                      DU1vdW50YWluIFZpZXcxDzANBgNVBAMTBkdvb2dsZTEZMBcGA1UECxMQR29vZ2xlIFdvcmtzcGFj
                      ZTELMAkGA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWEwggEiMA0GCSqGSIb3DQEBAQUAA4IB
                      DwAwggEKAoIBAQChApqlOBiQj3JkIdw2YlJjSCJbiLlCJx7lUrtIps5dayt1cgLm/1yRNLOqSILO
                      sduUa7gSnsD8tQtZ3NTENbm3omOfWtHGlaZMX8tfu2jkcYg8fWQnvSBFE4DI9Q6oy0xQZW4ssqGB
                      wzHvUcNpPQ4tKqxzS+hARywF6mmxmMUrRapf1LM+8vdrEiGGIF61yRDdHQCDBxmiDiH7PcO/yeCc
                      Bysvc1uzHqUakd3ua1JzDYlh7VHQLzT3aFYFkmpGxdRwI6jWmIuhrQ7AT6zET6cl9IuOtBXpCQuA
                      mWNRTPprhLDFZu5HIPq+hjbnwZPWEiW9qvJ0bpy2v7SwR96UNYHlAgMBAAEwDQYJKoZIhvcNAQEL
                      BQADggEBAAamJB1hIz1hgDcg9/ZMou+lfz5jxdZum0zLhXknpXW29qZ7p7G9O5UzbJrMaIOYYGNB
                      vMEJaKvt9JaSem6wT02rHpDnmz4kWj3aQo8ejp7KPjS4BqKiXbFZmCkwcp2MbBtPqkUpnIbC2DGj
                      UO/nqwvC8olz2z82uS4YwJxcHcpuErX/ldkrfO40SGWvh2jJWui4nGKRvVeN0ihOJg/xiXDiZej1
                      7PzbE1QQGrlnTjzWYmJmaCXlVpr9W2CFBY0hu3zKawd6Q6nwQ791+GU6KCxTJVb7fwJgBKqYsCvi
                      QiWxtT2bGnbt9HuXIkQ4mzsnLTWQ7W647dOH33XCkNlHVk4=</ds:X509Certificate>
                      				</ds:X509Data>
                      			</ds:KeyInfo>
                      		</ds:Signature>
                      		<saml2:Subject>
                      			<saml2:NameID Format="urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified">user@mail.it</saml2:NameID>
                      			<saml2:SubjectConfirmation Method="urn:oasis:names:tc:SAML:2.0:cm:bearer">
                      				<saml2:SubjectConfirmationData NotOnOrAfter="2025-09-05T11:06:16.063Z" Recipient="https://dev.selfcare.test.it/saml/acs"/>
                      			</saml2:SubjectConfirmation>
                      		</saml2:Subject>
                      		<saml2:Conditions NotBefore="2025-09-05T10:56:16.063Z" NotOnOrAfter="2025-09-05T11:06:16.063Z">
                      			<saml2:AudienceRestriction>
                      				<saml2:Audience>https://dev.selfcare.test.it</saml2:Audience>
                      			</saml2:AudienceRestriction>
                      		</saml2:Conditions>
                      		<saml2:AuthnStatement AuthnInstant="2025-08-26T10:59:16.000Z" SessionIndex="_9e1fa8afb432c3115dbc9c47848d4555">
                      			<saml2:AuthnContext>
                      				<saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:unspecified</saml2:AuthnContextClassRef>
                      			</saml2:AuthnContext>
                      		</saml2:AuthnStatement>
                      	</saml2:Assertion>
                      </saml2p:Response>
        """;

  private static final String SAML_BASE64 = "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiIHN0YW5kYWxvbmU9Im5vIj8+CiAgICAgICAgICAgICAgICAgICAgICA8c2FtbDJwOlJlc3BvbnNlCiAgICAgICAgICAgICAgICAgICAgICAJeG1sbnM6c2FtbDJwPSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6cHJvdG9jb2wiIERlc3RpbmF0aW9uPSJodHRwczovL2Rldi5zZWxmY2FyZS50ZXN0Lml0L3NhbWwvYWNzIiBJRD0iXzEyMzQ1Njc4OSIgSXNzdWVJbnN0YW50PSIyMDI1LTA5LTA1VDExOjAxOjE2LjA2M1oiIFZlcnNpb249IjIuMCI+CiAgICAgICAgICAgICAgICAgICAgICAJPHNhbWwyOklzc3VlcgogICAgICAgICAgICAgICAgICAgICAgCQl4bWxuczpzYW1sMj0idXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6Mi4wOmFzc2VydGlvbiI+aHR0cHM6Ly9hY2NvdW50cy50ZXN0LmNvbS9vL3NhbWwyP2lkcGlkPTEyMzQ1NgogICAgICAgICAgICAgICAgICAgICAgCTwvc2FtbDI6SXNzdWVyPgogICAgICAgICAgICAgICAgICAgICAgCTxzYW1sMnA6U3RhdHVzPgogICAgICAgICAgICAgICAgICAgICAgCQk8c2FtbDJwOlN0YXR1c0NvZGUgVmFsdWU9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDpzdGF0dXM6U3VjY2VzcyIvPgogICAgICAgICAgICAgICAgICAgICAgCTwvc2FtbDJwOlN0YXR1cz4KICAgICAgICAgICAgICAgICAgICAgIAk8c2FtbDI6QXNzZXJ0aW9uCiAgICAgICAgICAgICAgICAgICAgICAJCXhtbG5zOnNhbWwyPSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6YXNzZXJ0aW9uIiBJRD0iXzllMWZhOGFmYjQzMmMzMTE1ZGJjOWM0Nzg0OGQ0NTU1IiBJc3N1ZUluc3RhbnQ9IjIwMjUtMDktMDVUMTE6MDE6MTYuMDYzWiIgVmVyc2lvbj0iMi4wIj4KICAgICAgICAgICAgICAgICAgICAgIAkJPHNhbWwyOklzc3Vlcj5odHRwczovL2FjY291bnRzLnRlc3QuY29tL28vc2FtbDI/aWRwaWQ9MTIzNDU2PC9zYW1sMjpJc3N1ZXI+CiAgICAgICAgICAgICAgICAgICAgICAJCTxkczpTaWduYXR1cmUKICAgICAgICAgICAgICAgICAgICAgIAkJCXhtbG5zOmRzPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwLzA5L3htbGRzaWcjIj4KICAgICAgICAgICAgICAgICAgICAgIAkJCTxkczpTaWduZWRJbmZvPgogICAgICAgICAgICAgICAgICAgICAgCQkJCTxkczpDYW5vbmljYWxpemF0aW9uTWV0aG9kIEFsZ29yaXRobT0iaHR0cDovL3d3dy53My5vcmcvMjAwMS8xMC94bWwtZXhjLWMxNG4jIi8+CiAgICAgICAgICAgICAgICAgICAgICAJCQkJPGRzOlNpZ25hdHVyZU1ldGhvZCBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvMDQveG1sZHNpZy1tb3JlI3JzYS1zaGEyNTYiLz4KICAgICAgICAgICAgICAgICAgICAgIAkJCQk8ZHM6UmVmZXJlbmNlIFVSST0iI185ZTFmYThhZmI0MzJjMzExNWRiYzljNDc4NDhkNDU1NSI+CiAgICAgICAgICAgICAgICAgICAgICAJCQkJCTxkczpUcmFuc2Zvcm1zPgogICAgICAgICAgICAgICAgICAgICAgCQkJCQkJPGRzOlRyYW5zZm9ybSBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvMDkveG1sZHNpZyNlbnZlbG9wZWQtc2lnbmF0dXJlIi8+CiAgICAgICAgICAgICAgICAgICAgICAJCQkJCQk8ZHM6VHJhbnNmb3JtIEFsZ29yaXRobT0iaHR0cDovL3d3dy53My5vcmcvMjAwMS8xMC94bWwtZXhjLWMxNG4jIi8+CiAgICAgICAgICAgICAgICAgICAgICAJCQkJCTwvZHM6VHJhbnNmb3Jtcz4KICAgICAgICAgICAgICAgICAgICAgIAkJCQkJPGRzOkRpZ2VzdE1ldGhvZCBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvMDQveG1sZW5jI3NoYTI1NiIvPgogICAgICAgICAgICAgICAgICAgICAgCQkJCQk8ZHM6RGlnZXN0VmFsdWU+VlZEdUYzcklTUVhTUmxIeVIvdzdmdXpJOHAycFUzU25GNTcrc1czeTBiVT08L2RzOkRpZ2VzdFZhbHVlPgogICAgICAgICAgICAgICAgICAgICAgCQkJCTwvZHM6UmVmZXJlbmNlPgogICAgICAgICAgICAgICAgICAgICAgCQkJPC9kczpTaWduZWRJbmZvPgogICAgICAgICAgICAgICAgICAgICAgCQkJPGRzOlNpZ25hdHVyZVZhbHVlPmJVZlprRzRpWDJsK2JVWVh5ZDN3VUFhZk04bDBuamYwOXBQVlRHbGpnVGg1WmJyQjZzUU1iNk1VN0RXNVJ0NU0xdllySTBmTThFT0YKICAgICAgICAgICAgICAgICAgICAgIFZWVCtpQzhZM0pxSnZ0UThLRW1Hczh0cStmV09XY3g5MHdmY1c5RkE5VHVrZExjeVV3SEhUelN2MzdYV3BCRUhvU3duQXg5dHo2eDQKICAgICAgICAgICAgICAgICAgICAgIFhTR3paL2loWXZjV3BsVE9PZGphV0p5cFBOSHYrZW0vMmhnU3l3WEoyU0ZDdTZxeXN3VTVYL3loRktRVWxTTnpDWk0vS3dlSzZNY28KICAgICAgICAgICAgICAgICAgICAgIFlNNDVGQ1I3bzdwWUZ5TitGUkdBNzdvNEtGVHFkWWdtSFcrSTRZRlRkYll5TDFrVGNzaEpCRXJ3Z3JMUndIaFV0cFllcmlnY2cyNFMKICAgICAgICAgICAgICAgICAgICAgIHRZM3FYODJXbmlTV01sbk80ancyN0dFcGpiSFplUEFyYTQ4Tmp3PT08L2RzOlNpZ25hdHVyZVZhbHVlPgogICAgICAgICAgICAgICAgICAgICAgCQkJPGRzOktleUluZm8+CiAgICAgICAgICAgICAgICAgICAgICAJCQkJPGRzOlg1MDlEYXRhPgogICAgICAgICAgICAgICAgICAgICAgCQkJCQk8ZHM6WDUwOVN1YmplY3ROYW1lPlNUPUNhbGlmb3JuaWEsQz1VUyxPVT1UZXN0IFdvcmtzcGFjZSxDTj1UZXN0LEw9Um9tZSxPPVRlc3QgUy5wLkEuPC9kczpYNTA5U3ViamVjdE5hbWU+CiAgICAgICAgICAgICAgICAgICAgICAJCQkJCTxkczpYNTA5Q2VydGlmaWNhdGU+TUlJRGRqQ0NBbDZnQXdJQkFnSUdBWlJMcUhmS01BMEdDU3FHU0liM0RRRUJDd1VBTUh3eEZEQVNCZ05WQkFvVEMwZHZiMmRzWlNCSgogICAgICAgICAgICAgICAgICAgICAgYm1NdU1SWXdGQVlEVlFRSEV3MU5iM1Z1ZEdGcGJpQldhV1YzTVE4d0RRWURWUVFERXdaSGIyOW5iR1V4R1RBWEJnTlZCQXNURUVkdgogICAgICAgICAgICAgICAgICAgICAgYjJkc1pTQlhiM0pyYzNCaFkyVXhDekFKQmdOVkJBWVRBbFZUTVJNd0VRWURWUVFJRXdwRFlXeHBabTl5Ym1saE1CNFhEVEkxTURFdwogICAgICAgICAgICAgICAgICAgICAgT1RFMU1qRTFPRm9YRFRNd01ERXdPREUxTWpFMU9Gb3dmREVVTUJJR0ExVUVDaE1MUjI5dloyeGxJRWx1WXk0eEZqQVVCZ05WQkFjVAogICAgICAgICAgICAgICAgICAgICAgRFUxdmRXNTBZV2x1SUZacFpYY3hEekFOQmdOVkJBTVRCa2R2YjJkc1pURVpNQmNHQTFVRUN4TVFSMjl2WjJ4bElGZHZjbXR6Y0dGagogICAgICAgICAgICAgICAgICAgICAgWlRFTE1Ba0dBMVVFQmhNQ1ZWTXhFekFSQmdOVkJBZ1RDa05oYkdsbWIzSnVhV0V3Z2dFaU1BMEdDU3FHU0liM0RRRUJBUVVBQTRJQgogICAgICAgICAgICAgICAgICAgICAgRHdBd2dnRUtBb0lCQVFDaEFwcWxPQmlRajNKa0lkdzJZbEpqU0NKYmlMbENKeDdsVXJ0SXBzNWRheXQxY2dMbS8xeVJOTE9xU0lMTwogICAgICAgICAgICAgICAgICAgICAgc2R1VWE3Z1Nuc0Q4dFF0WjNOVEVOYm0zb21PZld0SEdsYVpNWDh0ZnUyamtjWWc4ZldRbnZTQkZFNERJOVE2b3kweFFaVzRzc3FHQgogICAgICAgICAgICAgICAgICAgICAgd3pIdlVjTnBQUTR0S3F4elMraEFSeXdGNm1teG1NVXJSYXBmMUxNKzh2ZHJFaUdHSUY2MXlSRGRIUUNEQnhtaURpSDdQY08veWVDYwogICAgICAgICAgICAgICAgICAgICAgQnlzdmMxdXpIcVVha2QzdWExSnpEWWxoN1ZIUUx6VDNhRllGa21wR3hkUndJNmpXbUl1aHJRN0FUNnpFVDZjbDlJdU90QlhwQ1F1QQogICAgICAgICAgICAgICAgICAgICAgbVdOUlRQcHJoTERGWnU1SElQcStoamJud1pQV0VpVzlxdkowYnB5MnY3U3dSOTZVTllIbEFnTUJBQUV3RFFZSktvWklodmNOQVFFTAogICAgICAgICAgICAgICAgICAgICAgQlFBRGdnRUJBQWFtSkIxaEl6MWhnRGNnOS9aTW91K2xmejVqeGRadW0wekxoWGtucFhXMjlxWjdwN0c5TzVVemJKck1hSU9ZWUdOQgogICAgICAgICAgICAgICAgICAgICAgdk1FSmFLdnQ5SmFTZW02d1QwMnJIcERubXo0a1dqM2FRbzhlanA3S1BqUzRCcUtpWGJGWm1Da3djcDJNYkJ0UHFrVXBuSWJDMkRHagogICAgICAgICAgICAgICAgICAgICAgVU8vbnF3dkM4b2x6Mno4MnVTNFl3SnhjSGNwdUVyWC9sZGtyZk80MFNHV3ZoMmpKV3VpNG5HS1J2VmVOMGloT0pnL3hpWERpWmVqMQogICAgICAgICAgICAgICAgICAgICAgN1B6YkUxUVFHcmxuVGp6V1ltSm1hQ1hsVnByOVcyQ0ZCWTBodTN6S2F3ZDZRNm53UTc5MStHVTZLQ3hUSlZiN2Z3SmdCS3FZc0N2aQogICAgICAgICAgICAgICAgICAgICAgUWlXeHRUMmJHbmJ0OUh1WElrUTRtenNuTFRXUTdXNjQ3ZE9IMzNYQ2tObEhWazQ9PC9kczpYNTA5Q2VydGlmaWNhdGU+CiAgICAgICAgICAgICAgICAgICAgICAJCQkJPC9kczpYNTA5RGF0YT4KICAgICAgICAgICAgICAgICAgICAgIAkJCTwvZHM6S2V5SW5mbz4KICAgICAgICAgICAgICAgICAgICAgIAkJPC9kczpTaWduYXR1cmU+CiAgICAgICAgICAgICAgICAgICAgICAJCTxzYW1sMjpTdWJqZWN0PgogICAgICAgICAgICAgICAgICAgICAgCQkJPHNhbWwyOk5hbWVJRCBGb3JtYXQ9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjEuMTpuYW1laWQtZm9ybWF0OnVuc3BlY2lmaWVkIj51c2VyQG1haWwuaXQ8L3NhbWwyOk5hbWVJRD4KICAgICAgICAgICAgICAgICAgICAgIAkJCTxzYW1sMjpTdWJqZWN0Q29uZmlybWF0aW9uIE1ldGhvZD0idXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6Mi4wOmNtOmJlYXJlciI+CiAgICAgICAgICAgICAgICAgICAgICAJCQkJPHNhbWwyOlN1YmplY3RDb25maXJtYXRpb25EYXRhIE5vdE9uT3JBZnRlcj0iMjAyNS0wOS0wNVQxMTowNjoxNi4wNjNaIiBSZWNpcGllbnQ9Imh0dHBzOi8vZGV2LnNlbGZjYXJlLnRlc3QuaXQvc2FtbC9hY3MiLz4KICAgICAgICAgICAgICAgICAgICAgIAkJCTwvc2FtbDI6U3ViamVjdENvbmZpcm1hdGlvbj4KICAgICAgICAgICAgICAgICAgICAgIAkJPC9zYW1sMjpTdWJqZWN0PgogICAgICAgICAgICAgICAgICAgICAgCQk8c2FtbDI6Q29uZGl0aW9ucyBOb3RCZWZvcmU9IjIwMjUtMDktMDVUMTA6NTY6MTYuMDYzWiIgTm90T25PckFmdGVyPSIyMDI1LTA5LTA1VDExOjA2OjE2LjA2M1oiPgogICAgICAgICAgICAgICAgICAgICAgCQkJPHNhbWwyOkF1ZGllbmNlUmVzdHJpY3Rpb24+CiAgICAgICAgICAgICAgICAgICAgICAJCQkJPHNhbWwyOkF1ZGllbmNlPmh0dHBzOi8vZGV2LnNlbGZjYXJlLnRlc3QuaXQ8L3NhbWwyOkF1ZGllbmNlPgogICAgICAgICAgICAgICAgICAgICAgCQkJPC9zYW1sMjpBdWRpZW5jZVJlc3RyaWN0aW9uPgogICAgICAgICAgICAgICAgICAgICAgCQk8L3NhbWwyOkNvbmRpdGlvbnM+CiAgICAgICAgICAgICAgICAgICAgICAJCTxzYW1sMjpBdXRoblN0YXRlbWVudCBBdXRobkluc3RhbnQ9IjIwMjUtMDgtMjZUMTA6NTk6MTYuMDAwWiIgU2Vzc2lvbkluZGV4PSJfOWUxZmE4YWZiNDMyYzMxMTVkYmM5YzQ3ODQ4ZDQ1NTUiPgogICAgICAgICAgICAgICAgICAgICAgCQkJPHNhbWwyOkF1dGhuQ29udGV4dD4KICAgICAgICAgICAgICAgICAgICAgIAkJCQk8c2FtbDI6QXV0aG5Db250ZXh0Q2xhc3NSZWY+dXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6Mi4wOmFjOmNsYXNzZXM6dW5zcGVjaWZpZWQ8L3NhbWwyOkF1dGhuQ29udGV4dENsYXNzUmVmPgogICAgICAgICAgICAgICAgICAgICAgCQkJPC9zYW1sMjpBdXRobkNvbnRleHQ+CiAgICAgICAgICAgICAgICAgICAgICAJCTwvc2FtbDI6QXV0aG5TdGF0ZW1lbnQ+CiAgICAgICAgICAgICAgICAgICAgICAJPC9zYW1sMjpBc3NlcnRpb24+CiAgICAgICAgICAgICAgICAgICAgICA8L3NhbWwycDpSZXNwb25zZT4=";
  private static final String FAKE_SAML_RESPONSE = "fake-saml-response";
  private static final String FAKE_IDP_CERT = "test";
  private static final long FAKE_TIME_INTERVAL = 10L;

  @Test
  public void testBasicXmlParsing() {
    System.out.println("=== TEST XML PARSING ===");

    try {
      String cleaned = samlValidator.cleanXmlContent(TEST_SAML);
      System.out.println("✓ XML cleaning completed");

      Method parseMethod = SamlValidator.class.getDeclaredMethod("parseXmlDocument", String.class);
      parseMethod.setAccessible(true);

      Document doc = (Document) parseMethod.invoke(samlValidator, cleaned);

      if (doc != null) {
        System.out.println("✓ Document parsed successfully");
        System.out.println("Root element: " + doc.getDocumentElement().getTagName());
        System.out.println("Namespace: " + doc.getDocumentElement().getNamespaceURI());
      } else {
        System.out.println("✗ Document is null!");
      }

    } catch (Exception e) {
      System.out.println("✗ Error during parsing: " + e.getMessage());
      e.printStackTrace();
    }
  }

  @Test
  public void testParseSamlBase64() {
    System.out.println("=== TEST PARSING XML FROM Base64 ===");

    try {

      byte[] saml = Base64.getDecoder().decode(SAML_BASE64.getBytes(StandardCharsets.UTF_8));
      String samlResponseXML = new String(saml, StandardCharsets.UTF_8);

      String cleaned = samlValidator.cleanXmlContent(samlResponseXML);
      System.out.println("✓ XML cleaning completed");

      Method parseMethod = SamlValidator.class.getDeclaredMethod("parseXmlDocument", String.class);
      parseMethod.setAccessible(true);

      Document doc = (Document) parseMethod.invoke(samlValidator, cleaned);

      if (doc != null) {
        System.out.println("✓ Document parsed successfully");
        System.out.println("Root element: " + doc.getDocumentElement().getTagName());
        System.out.println("Namespace: " + doc.getDocumentElement().getNamespaceURI());
      } else {
        System.out.println("✗ Document is null!");
      }

    } catch (Exception e) {
      System.out.println("✗ Error during parsing: " + e.getMessage());
      e.printStackTrace();
    }

  }

  @Test
  public void testEmptyXml() {
    System.out.println("=== TEST EMPTY XML ===");

    RuntimeException thrown = assertThrows(SamlSignatureException.class, () -> {
      samlValidator.validateSamlResponse("", "", 190);
    });

    assertEquals("Validation Error", thrown.getMessage(), "The exception message should be propagated");
  }

  /**
   * Defines a test profile to supply mock configuration properties.
   */
  public static class SAMLServiceTestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
      return Map.of(
        "saml.idp.cert", FAKE_IDP_CERT,
        "saml.time.interval", String.valueOf(FAKE_TIME_INTERVAL)
      );
    }
  }

  @Test
  public void testValidate_Success() {
    // Arrange: Configure the mock validator to return a successful Uni<Boolean>
    when(samlValidator.validateSamlResponseAsync(FAKE_SAML_RESPONSE, FAKE_IDP_CERT, FAKE_TIME_INTERVAL))
      .thenReturn(Uni.createFrom().item(true));

    // Act: Call the service method
    Uni<Boolean> resultUni = samlService.validate(FAKE_SAML_RESPONSE);

    // Assert: Check that the result is true and the validator was called correctly
    Boolean result = resultUni.await().indefinitely();
    assertTrue(result, "Validation should be successful");

    verify(samlValidator).validateSamlResponseAsync(FAKE_SAML_RESPONSE, FAKE_IDP_CERT, FAKE_TIME_INTERVAL);
  }

  @Test
  public void testValidate_Failure() {
    // Arrange: Configure the mock validator to return a failed Uni<Boolean>
    when(samlValidator.validateSamlResponseAsync(FAKE_SAML_RESPONSE, FAKE_IDP_CERT, FAKE_TIME_INTERVAL))
      .thenReturn(Uni.createFrom().item(false));

    // Act: Call the service method
    Uni<Boolean> resultUni = samlService.validate(FAKE_SAML_RESPONSE);

    // Assert: Check that the result is false and the validator was called correctly
    Boolean result = resultUni.await().indefinitely();
    assertFalse(result, "Validation should fail");

    verify(samlValidator).validateSamlResponseAsync(FAKE_SAML_RESPONSE, FAKE_IDP_CERT, FAKE_TIME_INTERVAL);
  }

  @Test
  public void testValidate_Exception() {
    // Arrange: Configure the mock validator to return a failed Uni with an exception
    RuntimeException mockException = new RuntimeException("Validation Error");
    when(samlValidator.validateSamlResponseAsync(FAKE_SAML_RESPONSE, FAKE_IDP_CERT, FAKE_TIME_INTERVAL))
      .thenReturn(Uni.createFrom().failure(mockException));

    // Act & Assert: Check that the exception is propagated correctly
    RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
      samlService.validate(FAKE_SAML_RESPONSE).await().indefinitely();
    });

    assertEquals("Validation Error", thrown.getMessage(), "The exception message should be propagated");

    verify(samlValidator).validateSamlResponseAsync(FAKE_SAML_RESPONSE, FAKE_IDP_CERT, FAKE_TIME_INTERVAL);
  }
}
