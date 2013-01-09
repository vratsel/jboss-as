/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.jca.moduledeployment.ij;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.jca.moduledeployment.ModuleDeploymentTestCaseSetup;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.annotation.Resource;
import javax.resource.cci.ConnectionFactory;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.junit.Assert.assertNotNull;

/**
 * AS7-5768 -Support for RA module deployment
 * 
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a>
 */
@RunWith(Arquillian.class)
@ServerSetup(ModuleDeploymentIJTestCase.ModuleIJDeploymentTestCaseSetup.class)
public class ModuleDeploymentIJTestCase extends ContainerResourceMgmtTestBase {

	static class ModuleIJDeploymentTestCaseSetup extends
			ModuleDeploymentTestCaseSetup {
		private ModelNode address;

		@Override
		public void doSetup(ManagementClient managementClient) throws Exception {
			addModule("org/jboss/ironjacamar/ra16outij2", "ra16outij2.rar");

			address = new ModelNode();
			address.add("subsystem", "resource-adapters");
			address.add("resource-adapter", "ra16outij2");
			address.protect();

			final ModelNode operation = new ModelNode();
			operation.get(OP).set("add");
			operation.get(OP_ADDR).set(address);
			operation.get("module").set("org.jboss.ironjacamar.ra16outij2");
			executeOperation(operation);

		}

		@Override
		public void tearDown(ManagementClient managementClient,
				String containerId) throws Exception {
			remove(address);
			removeModule("org/jboss/ironjacamar/ra16outij2");
		}

	}

	/**
	 * Define the deployment
	 * 
	 * @return The deployment archive
	 */
	@Deployment
	public static JavaArchive createDeployment() throws Exception {

		JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "multiple.jar");
		ja.addClasses(ModuleDeploymentIJTestCase.class,
				MgmtOperationException.class, XMLElementReader.class,
				XMLElementWriter.class, ModuleIJDeploymentTestCaseSetup.class,
				ModuleDeploymentTestCaseSetup.class);

		ja.addPackage(AbstractMgmtTestBase.class.getPackage());

		ja.addAsManifestResource(
				new StringAsset(
						"Dependencies: org.jboss.as.controller-client,org.jboss.dmr,org.jboss.as.cli\n"),
				"MANIFEST.MF");
		return ja;
	}

	@Resource(mappedName = "java:/testMe2")
	private ConnectionFactory connectionFactory;

	/**
	 * Test configuration - if all properties propagated to the model
	 * 
	 * @throws Throwable
	 *             Thrown if case of an error
	 */
	@Test
	public void testConfiguration() throws Throwable {
		assertNotNull(connectionFactory);
		assertNotNull(connectionFactory.getConnection());
	}

}
