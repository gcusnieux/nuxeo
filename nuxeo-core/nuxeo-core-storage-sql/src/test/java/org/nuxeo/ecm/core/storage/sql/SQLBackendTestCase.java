/*
 * (C) Copyright 2007-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.storage.sql;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author Florent Guillaume
 */
public abstract class SQLBackendTestCase extends NXRuntimeTestCase {

    public Repository repository;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        SQLBackendHelper.setUpRepository();

        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        assertNotNull(schemaManager);

        RepositoryDescriptor descriptor = prepareDescriptor();
        repository = new RepositoryImpl(descriptor, schemaManager);
    }

    @Override
    protected void tearDown() throws Exception {
        if (repository != null) {
            repository.close();
        }
        SQLBackendHelper.tearDownRepository();
        super.tearDown();
    }

    protected RepositoryDescriptor prepareDescriptor() {
        switch (SQLBackendHelper.DATABASE) {
        case DERBY:
            return prepareDescriptorDerby();
        case POSTGRESQL:
            return prepareDescriptorPostgreSQL();
        }
        throw new RuntimeException(); // not reached
    }

    protected RepositoryDescriptor prepareDescriptorDerby() {
        RepositoryDescriptor descriptor = new RepositoryDescriptor();
        String className = org.apache.derby.jdbc.EmbeddedXADataSource.class.getName();
        descriptor.xaDataSourceName = className;
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("createDatabase", "create");
        properties.put("databaseName", new File(
                SQLBackendHelper.DERBY_DIRECTORY).getAbsolutePath());
        properties.put("user", "sa");
        properties.put("password", "");
        descriptor.properties = properties;
        return descriptor;
    }

    protected RepositoryDescriptor prepareDescriptorPostgreSQL() {
        RepositoryDescriptor descriptor = new RepositoryDescriptor();
        descriptor.xaDataSourceName = "org.postgresql.xa.PGXADataSource";
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("ServerName", SQLBackendHelper.PG_HOST);
        properties.put("PortNumber/Integer", SQLBackendHelper.PG_PORT);
        properties.put("DatabaseName", SQLBackendHelper.PG_DATABASE);
        properties.put("User", SQLBackendHelper.PG_DATABASE_OWNER);
        properties.put("Password", SQLBackendHelper.PG_DATABASE_PASSWORD);
        descriptor.properties = properties;
        return descriptor;
    }

}