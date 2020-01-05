package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.model.WorkspaceResponse;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * WorkspaceResponseListResponse
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T14:10:50.813-05:00")

public class WorkspaceResponseListResponse   {
  @JsonProperty("items")
  private List<WorkspaceResponse> items = new ArrayList<WorkspaceResponse>();

  public WorkspaceResponseListResponse items(List<WorkspaceResponse> items) {
    this.items = items;
    return this;
  }

  public WorkspaceResponseListResponse addItemsItem(WorkspaceResponse itemsItem) {
    this.items.add(itemsItem);
    return this;
  }

   /**
   * Get items
   * @return items
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull

  @Valid

  public List<WorkspaceResponse> getItems() {
    return items;
  }

  public void setItems(List<WorkspaceResponse> items) {
    this.items = items;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WorkspaceResponseListResponse workspaceResponseListResponse = (WorkspaceResponseListResponse) o;
    return Objects.equals(this.items, workspaceResponseListResponse.items);
  }

  @Override
  public int hashCode() {
    return Objects.hash(items);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkspaceResponseListResponse {\n");
    
    sb.append("    items: ").append(toIndentedString(items)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

