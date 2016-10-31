package org.eclipse.aether.transport.wagon;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.InputData;
import org.apache.maven.wagon.OutputData;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.StreamWagon;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.resource.Resource;

/**
 */
public class MemStreamWagon
    extends StreamWagon
    implements Configurable
{

    private Map<String, String> fs;

    private Properties headers;

    private Object config;

    public void setConfiguration( Object config )
    {
        this.config = config;
    }

    public Object getConfiguration()
    {
        return config;
    }

    public void setHttpHeaders( Properties httpHeaders )
    {
        headers = httpHeaders;
    }

    @Override
    protected void openConnectionInternal()
        throws ConnectionException, AuthenticationException
    {
        fs =
            MemWagonUtils.openConnection( this, getAuthenticationInfo(),
                                          getProxyInfo( "mem", getRepository().getHost() ), headers );
    }

    @Override
    public void closeConnection()
        throws ConnectionException
    {
        fs = null;
    }

    private String getData( String resource )
    {
        return fs.get( URI.create( resource ).getSchemeSpecificPart() );
    }

    @Override
    public boolean resourceExists( String resourceName )
        throws TransferFailedException, AuthorizationException
    {
        String data = getData( resourceName );
        return data != null;
    }

    @Override
    public void fillInputData( InputData inputData )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        String data = getData( inputData.getResource().getName() );
        if ( data == null )
        {
            throw new ResourceDoesNotExistException( "Missing resource: " + inputData.getResource().getName() );
        }
        byte[] bytes = data.getBytes( StandardCharsets.UTF_8 );
        inputData.getResource().setContentLength( bytes.length );
        inputData.setInputStream( new ByteArrayInputStream( bytes ) );
    }

    @Override
    public void fillOutputData( OutputData outputData )
        throws TransferFailedException
    {
        outputData.setOutputStream( new ByteArrayOutputStream() );
    }

    @Override
    protected void finishPutTransfer( Resource resource, InputStream input, OutputStream output )
        throws TransferFailedException, AuthorizationException, ResourceDoesNotExistException
    {
        String data = new String( ( (ByteArrayOutputStream) output ).toByteArray(), StandardCharsets.UTF_8 );
        fs.put( URI.create( resource.getName() ).getSchemeSpecificPart(), data );
    }

}
