package it.pagopa.selfcare.onboarding.client.transport;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.interceptor.InvocationContext;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import java.lang.reflect.Method;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.client.api.ClientMultipartForm;
import org.junit.jupiter.api.Test;

class RequestBodiesTest {

    interface Client {
        @GET
        @Path("/a/{id}")
        String read(@PathParam("id") String id, @QueryParam("q") String query, @HeaderParam("h") String header);

        @HEAD
        @Path("/a/{id}")
        void head(@PathParam("id") String id, @RestQuery String name);

        @DELETE
        @Path("/a/{id}")
        void delete(@PathParam("id") String id);

        @PUT
        @Path("/a/{id}")
        void putWithoutBody(@PathParam("id") String id);

        @POST
        @Path("/a")
        String post(Object body);

        @POST
        @Path("/a/{id}")
        String postWithHeader(@PathParam("id") String id, @HeaderParam("h") String header, Object body);

        @PUT
        @Path("/a/{id}/file")
        String upload(@PathParam("id") String id, ClientMultipartForm form);

        @POST
        @Path("/a/form")
        String formField(@RestForm String field);

        @POST
        @Path("/a/form")
        String jaxrsFormField(@jakarta.ws.rs.FormParam("field") String field);
    }

    private static boolean carriesBody(String methodName, Object... arguments) {
        Method method = null;
        for (Method candidate : Client.class.getMethods()) {
            if (candidate.getName().equals(methodName)) {
                method = candidate;
            }
        }
        InvocationContext context = mock(InvocationContext.class);
        when(context.getMethod()).thenReturn(method);
        when(context.getParameters()).thenReturn(arguments);
        return RequestBodies.carriesBody(context);
    }

    @Test
    void readsAndDeletesWithoutEntity_haveNoBody() {
        assertFalse(carriesBody("read", "id", "q", "h"));
        assertFalse(carriesBody("head", "id", "name"));
        assertFalse(carriesBody("delete", "id"));
        assertFalse(carriesBody("putWithoutBody", "id"));
    }

    @Test
    void anEntityArgument_isABody() {
        assertTrue(carriesBody("post", new Object()));
        assertTrue(carriesBody("postWithHeader", "id", "h", new Object()));
        assertTrue(carriesBody("upload", "id", ClientMultipartForm.create()));
    }

    @Test
    void aFormField_isABody() {
        assertTrue(carriesBody("formField", "value"));
        assertTrue(carriesBody("jaxrsFormField", "value"));
    }

    @Test
    void anAbsentEntity_isNotABody() {
        assertFalse(carriesBody("post", (Object) null));
        assertFalse(carriesBody("upload", "id", null));
    }
}
