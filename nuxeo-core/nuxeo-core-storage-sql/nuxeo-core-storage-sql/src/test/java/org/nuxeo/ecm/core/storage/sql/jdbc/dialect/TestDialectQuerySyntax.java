/*
 * (C) Copyright 2008-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql.jdbc.dialect;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.DatabaseMetaData;

import junit.framework.TestCase;

import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.storage.sql.BinaryManager;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.jdbc.QueryMaker.QueryMakerException;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.Dialect.FulltextQuery;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.Dialect.FulltextQuery.Op;

public class TestDialectQuerySyntax extends TestCase {

    protected static class DatabaseMetaDataInvocationHandler implements
            InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            String name = method.getName();
            if (name.equals("storesUpperCaseIdentifiers")) {
                return Boolean.FALSE;
            } else if (name.equals("getDatabaseMajorVersion")) {
                return Integer.valueOf(0);
            } else if (name.equals("getDatabaseMinorVersion")) {
                return Integer.valueOf(0);
            }
            return null;
        }
    }

    public static DatabaseMetaData getDatabaseMetaData() {
        return (DatabaseMetaData) Proxy.newProxyInstance(
                Session.class.getClassLoader(),
                new Class<?>[] { DatabaseMetaData.class },
                new DatabaseMetaDataInvocationHandler());
    }

    public DatabaseMetaData metadata;

    public BinaryManager binaryManager;

    public RepositoryDescriptor repositoryDescriptor;

    public Dialect dialect;

    @Override
    public void setUp() {
        metadata = getDatabaseMetaData();
        binaryManager = null;
        repositoryDescriptor = new RepositoryDescriptor();
    }

    protected static void assertFulltextException(String query)
            throws Exception {
        try {
            Dialect.analyzeFulltextQuery(query);
            fail("Query should fail: " + query);
        } catch (QueryMakerException e) {
            // ok
        }
    }

    protected static void dumpFulltextQuery(FulltextQuery ft, StringBuilder buf) {
        if (ft.op == Op.AND || ft.op == Op.OR) {
            assertNull(ft.word);
            buf.append('[');
            for (int i = 0; i < ft.terms.size(); i++) {
                if (i != 0) {
                    buf.append(' ');
                    buf.append(ft.op.name());
                    buf.append(' ');
                }
                dumpFulltextQuery(ft.terms.get(i), buf);
            }
            buf.append(']');
            return;
        } else {
            assertNull(ft.terms);
            if (ft.op == Op.NOTWORD) {
                buf.append('~');
            }
            // TODO phrase
            buf.append(ft.word);
        }
    }

    protected static void assertFulltextQuery(String expected, String query)
            throws Exception {
        StringBuilder buf = new StringBuilder();
        FulltextQuery ftQuery = Dialect.analyzeFulltextQuery(query);
        if (ftQuery == null) {
            assertNull(expected);
            return;
        } else {
            dumpFulltextQuery(ftQuery, buf);
            assertEquals(expected, buf.toString());
        }
    }

    public void testAnalyzeFulltextQuery() throws Exception {
        dialect = new DialectH2(metadata, binaryManager, repositoryDescriptor);

        // invalid queries

        assertFulltextException("OR foo");
        assertFulltextException("OR foo bar");
        assertFulltextException("foo OR");
        assertFulltextException("foo bar OR");
        assertFulltextException("foo OR OR bar");
        assertFulltextException("foo bar OR OR baz");
        assertFulltextException("foo + bar");
        assertFulltextException("foo - bar");

        // ok queries

        assertFulltextQuery(null, "");
        assertFulltextQuery(null, "  ");
        assertFulltextQuery("foo", "foo");
        assertFulltextQuery("foo", " foo ");
        assertFulltextQuery("[foo AND bar]", "foo bar");
        assertFulltextQuery("[foo AND bar AND baz]", "foo bar baz");
        assertFulltextQuery("[foo AND ~bar]", "foo -bar");
        assertFulltextQuery("[foo AND baz AND ~bar]", "foo -bar baz");
        assertFulltextQuery("[foo AND ~bar AND ~baz]", "foo -bar -baz");
        assertFulltextQuery("[bar AND ~foo]", "-foo bar");
        assertFulltextQuery("[bar AND baz AND ~foo]", "-foo bar baz");

        // queries with OR

        assertFulltextQuery("[foo OR bar]", "foo OR bar");
        assertFulltextQuery("[foo OR [bar AND baz]]", "foo OR bar baz");
        assertFulltextQuery("[[foo AND bar] OR baz]", "foo bar OR baz");
        assertFulltextQuery("[foo OR bar OR baz]", "foo OR bar OR baz");
        assertFulltextQuery("[foo OR [bar AND baz] OR gee]",
                "foo OR bar baz OR gee");
        assertFulltextQuery("[[bar AND ~foo] OR baz]", "-foo bar OR baz");
        assertFulltextQuery("[foo OR [bar AND ~baz]]", "foo OR bar -baz");

        // queries containing suppressed terms

        assertFulltextQuery(null, "-foo");
        assertFulltextQuery(null, "-foo -bar");
        assertFulltextQuery("bar", "-foo OR bar");
        assertFulltextQuery("foo", "foo OR -bar");
        assertFulltextQuery(null, "-foo OR -bar");
        assertFulltextQuery("foo", "foo OR -bar -baz");
        assertFulltextQuery("baz", "-foo -bar OR baz");
    }

    protected void assertDialectFT(String expected, String query) {
        assertEquals(expected, dialect.getDialectFulltextQuery(query));
    }

    public void testH2() throws Exception {
        dialect = new DialectH2(metadata, binaryManager, repositoryDescriptor);
        assertDialectFT("DONTMATCHANYTHINGFOREMPTYQUERY", "");
        assertDialectFT("foo", "foo");
        assertDialectFT("(foo AND bar)", "foo bar");
        assertDialectFT("(foo NOT bar)", "foo -bar");
        assertDialectFT("(bar NOT foo)", "-foo bar");
        assertDialectFT("(foo OR bar)", "foo OR bar");
        assertDialectFT("foo", "foo OR -bar");
        assertDialectFT("((foo AND bar) OR baz)", "foo bar OR baz");
        assertDialectFT("((bar NOT foo) OR baz)", "-foo bar OR baz");
        assertDialectFT("((foo NOT bar) OR baz)", "foo -bar OR baz");
    }

    public void testPostgreSQL() throws Exception {
        dialect = new DialectPostgreSQL(metadata, binaryManager,
                repositoryDescriptor);
        assertDialectFT("", "-foo");
        assertDialectFT("foo", "foo");
        assertDialectFT("(foo & bar)", "foo bar ");
        assertDialectFT("(foo & bar)", "foo & bar"); // compat
        assertDialectFT("(foo & ! bar)", "foo -bar");
        assertDialectFT("(bar & ! foo)", "-foo bar");
        assertDialectFT("(foo | bar)", "foo OR bar");
        assertDialectFT("foo", "foo OR -bar");
        assertDialectFT("((foo & bar) | baz)", "foo bar OR baz");
        assertDialectFT("((bar & ! foo) | baz)", "-foo bar OR baz");
        assertDialectFT("((foo & ! bar) | baz)", "foo -bar OR baz");
    }

    public void testMySQL() throws Exception {
        dialect = new DialectMySQL(metadata, binaryManager,
                repositoryDescriptor);
        assertDialectFT("DONTMATCHANYTHINGFOREMPTYQUERY", "-foo");
        assertDialectFT("foo", "foo");
        assertDialectFT("(+foo +bar)", "foo bar");
        assertDialectFT("(+foo -bar)", "foo -bar");
        assertDialectFT("(+bar -foo)", "-foo bar");
        assertDialectFT("(foo bar)", "foo OR bar");
        assertDialectFT("foo", "foo OR -bar");
        assertDialectFT("((+foo +bar) baz)", "foo bar OR baz");
        assertDialectFT("((+bar -foo) baz)", "-foo bar OR baz");
        assertDialectFT("((+foo -bar) baz)", "foo -bar OR baz");
    }

    public void testOracle() throws Exception {
        dialect = new DialectOracle(metadata, binaryManager,
                repositoryDescriptor);
        assertDialectFT("DONTMATCHANYTHINGFOREMPTYQUERY", "-foo");
        assertDialectFT("foo", "foo");
        assertDialectFT("foo%", "foo*"); // special
        assertDialectFT("(foo AND bar)", "foo bar");
        assertDialectFT("(foo NOT bar)", "foo -bar");
        assertDialectFT("(bar NOT foo)", "-foo bar");
        assertDialectFT("(foo OR bar)", "foo OR bar");
        assertDialectFT("foo", "foo OR -bar");
        assertDialectFT("((foo AND bar) OR baz)", "foo bar OR baz");
        assertDialectFT("((bar NOT foo) OR baz)", "-foo bar OR baz");
        assertDialectFT("((foo NOT bar) OR baz)", "foo -bar OR baz");
    }

    public void testSQLServer() throws Exception {
        dialect = new DialectSQLServer(metadata, binaryManager,
                repositoryDescriptor);
        assertDialectFT("DONTMATCHANYTHINGFOREMPTYQUERY", "-foo");
        assertDialectFT("foo", "foo");
        assertDialectFT("(foo & bar)", "foo bar");
        assertDialectFT("(foo &! bar)", "foo -bar");
        assertDialectFT("(bar &! foo)", "-foo bar");
        assertDialectFT("(foo | bar)", "foo OR bar");
        assertDialectFT("foo", "foo OR -bar");
        assertDialectFT("((foo & bar) | baz)", "foo bar OR baz");
        assertDialectFT("((bar &! foo) | baz)", "-foo bar OR baz");
        assertDialectFT("((foo &! bar) | baz)", "foo -bar OR baz");
    }

}