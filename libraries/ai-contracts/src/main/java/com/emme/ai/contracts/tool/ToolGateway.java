package com.emme.ai.contracts.tool;

import com.emme.kernel.context.AiExecutionContext;

/** Port for governed tool execution through application use cases or authenticated MCP clients. */
public interface ToolGateway {

  ToolResult execute(ToolExecutionRequest request, AiExecutionContext context);
}
