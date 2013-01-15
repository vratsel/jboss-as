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
package org.jboss.as.test.integration.jca.moduledeployment.jar;

import java.io.File;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.jca.moduledeployment.ModuleDeploymentTestCaseSetup;
import org.jboss.as.test.integration.jca.rar.MultipleConnectionFactory1;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.integration.management.util.ModelUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
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
 * 
 * Tests for module deployment of resource adapter archive in 
 * uncompressed form with classes, packed in .jar file
 * 
 * Structure of module is:
 * modulename
 * modulename/main 
 * modulename/main/module.xml
 * modulename/main/META-INF
 * modulename/main/META-INF/ra.xml
 * modulename/main/module.jar
 */
@RunWith(Arquillian.class)
@ServerSetup(PureJarTestCase.ModuleAcDeploymentTestCaseSetup.class)
public class PureJarTestCase extends
		ContainerResourceMgmtTestBase {

	private static ModelNode address;

	static class ModuleAcDeploymentTestCaseSetup extends
			ModuleDeploymentTestCaseSetup {
		
		/**
		 * Creates module structure for uncompressed RA archive.
		 * RA classes are packed in .jar archive
		 * @throws Exception
		 */
		private void exportArchive() throws Exception {
			addModule("org/jboss/ironjacamar/ra16out");
			ResourceAdapterArchive rar = ShrinkWrap
					.create(ResourceAdapterArchive.class);
			JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ra16out.jar");
			jar.addPackage(MultipleConnectionFactory1.class.getPackage());
			rar.addAsManifestResource(
					PureJarTestCase.class.getPackage(), "ra.xml", "ra.xml");
			rar.as(ExplodedExporter.class).exportExploded(testModuleRoot, "main");

			copyFile(new File(slot, "ra16out.jar"), jar.as(ZipExporter.class).exportAsInputStream());
		}

		@Override
		public void doSetup(ManagementClient managementClient) throws Exception {

			exportArchive();

			address = new ModelNode();
			address.add("subsystem", "resource-adapters");
			address.add("resource-adapter", "org.jboss.ironjacamar.ra16out");
			address.protect();

			final ModelNode operation = new ModelNode();
			operation.get(OP).set("add");
			operation.get(OP_ADDR).set(address);
			operation.get("module").set("org.jboss.ironjacamar.ra16out");

			final ModelNode address1 = address.clone();
			address1.add("connection-definitions", "java:/testMeRA");
			address1.protect();

			final ModelNode operation1 = new ModelNode();
			operation1.get(OP).set("add");
			operation1.get(OP_ADDR).set(address1);
			operation1
					.get("class-name")
					.set("org.jboss.as.test.integration.jca.rar.MultipleManagedConnectionFactory1");
			operation1.get("jndi-name").set("java:/testMeRA");
			ModelNode[] operations = new ModelNode[] { operation, operation1 };

			executeOperation(ModelUtil.createCompositeNode(operations));

		}

		@Override
		public void tearDown(ManagementClient managementClient,
				String containerId) throws Exception {
			remove(address);
			removeModule("org/jboss/ironjacamar/ra16out");
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
		ja.addClasses(PureJarTestCase.class,
				MgmtOperationException.class, XMLElementReader.class,
				XMLElementWriter.class, ModuleAcDeploymentTestCaseSetup.class,
				ModuleDeploymentTestCaseSetup.class);

		ja.addPackage(AbstractMgmtTestBase.class.getPackage()).addPackage(
				MultipleConnectionFactory1.class.getPackage());

		ja.addAsManifestResource(
				new StringAsset(
						"Dependencies: org.jboss.as.controller-client,org.jboss.dmr,org.jboss.as.cli\n"),
				"MANIFEST.MF");
		return ja;
	}

	@Resource(mappedName = "java:/testMeRA")
	private ConnectionFactory connectionFactory;

	/**
	 * Test configuration 
	 * 
	 * @throws Throwable in case of error
	 */
	@Test
	public void testConfiguration() throws Throwable {
		assertNotNull(connectionFactory);
		assertNotNull(connectionFactory.getConnection());
	}

	/**
	 * Tests connection in pool
	 * @throws Exception in case of error
	 */
	@Test
	@RunAsClient
	public void testConnection() throws Exception {
		final ModelNode address1 = address.clone();
		address1.add("connection-definitions", "java:/testMeRA");
		address1.protect();

		final ModelNode operation1 = new ModelNode();
		operation1.get(OP).set("test-connection-in-pool");
		operation1.get(OP_ADDR).set(address1);
		executeOperation(operation1);
	}
}
