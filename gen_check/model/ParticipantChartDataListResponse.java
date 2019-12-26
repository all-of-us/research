package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.model.ParticipantChartData;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * ParticipantChartDataListResponse
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T14:42:01.876-06:00")

public class ParticipantChartDataListResponse   {
  @JsonProperty("items")
  private List<ParticipantChartData> items = new ArrayList<ParticipantChartData>();

  public ParticipantChartDataListResponse items(List<ParticipantChartData> items) {
    this.items = items;
    return this;
  }

  public ParticipantChartDataListResponse addItemsItem(ParticipantChartData itemsItem) {
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

  public List<ParticipantChartData> getItems() {
    return items;
  }

  public void setItems(List<ParticipantChartData> items) {
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
    ParticipantChartDataListResponse participantChartDataListResponse = (ParticipantChartDataListResponse) o;
    return Objects.equals(this.items, participantChartDataListResponse.items);
  }

  @Override
  public int hashCode() {
    return Objects.hash(items);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ParticipantChartDataListResponse {\n");
    
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

