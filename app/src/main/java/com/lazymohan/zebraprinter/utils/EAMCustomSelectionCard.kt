package com.lazymohan.zebraprinter.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tarkalabs.tarkaui.components.VerticalSpacer
import com.tarkalabs.tarkaui.icons.ChevronRight24
import com.tarkalabs.tarkaui.icons.TarkaIcons.Regular
import com.tarkalabs.tarkaui.theme.TUITheme

@Composable
fun EAMCustomSelectionCard(
  modifier: Modifier = Modifier,
  label: String? = null,
  description: String? = null,
  showTrailingIcon: Boolean = false,
  isRoundedCornerShape: Boolean = false,
  tags: EAMCustomSelectionCardTags = EAMCustomSelectionCardTags(),
  onCardClicked: () -> Unit
) {
  Row(
    modifier = modifier
      .testTag(tags.parentTag)
      .clip(if (isRoundedCornerShape) RoundedCornerShape(16.dp) else RoundedCornerShape(0.dp))
      .background(TUITheme.colors.surface)
      .clickable {
        onCardClicked()
      }
      .padding(horizontal = 16.dp, vertical = 12.dp)
  ) {
    Column(
      modifier = Modifier.weight(1f)
    ) {
      label?.let {
        Text(
          modifier = Modifier.testTag(tags.labelTag),
          text = it,
          color = TUITheme.colors.inputTextDim.copy(alpha = 0.7f),
          style = TUITheme.typography.body7,
          lineHeight = 18.sp
        )
      }
      description?.let {
        Text(
          modifier = Modifier.testTag(tags.descriptionTag),
          text = description,
          color = TUITheme.colors.inputText,
          style = TUITheme.typography.heading6,
          lineHeight = 20.sp
        )
      }
    }
    
    if (showTrailingIcon) {
      Icon(
        modifier = Modifier
          .align(Alignment.CenterVertically)
          .size(24.dp)
          .testTag(tags.trailingFrontArrowIconTag),
        painter = painterResource(id = Regular.ChevronRight24.iconRes),
        contentDescription = Regular.ChevronRight24.contentDescription,
        tint = TUITheme.colors.utilityOutline
      )
    }
  }
}

data class EAMCustomSelectionCardTags(
  val parentTag: String = "EAMCustomSelectionCard",
  val labelTag: String = "EAMCustomSelectionCard_LabelTag",
  val descriptionTag: String = "EAMCustomSelectionCard_DescriptionTag",
  val trailingFrontArrowIconTag: String = "EAMCustomSelectionCard_TrailingFrontArrowIconTag"
)

@Preview @Composable
fun EAMCustomSelectionCardPreview() {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(15.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    EAMCustomSelectionCard(
      label = "itemNum",
      description = "item Description",
      showTrailingIcon = false
    ) {}
    VerticalSpacer(space = 14)
    EAMCustomSelectionCard(
      label = null,
      description = "Description"
    ) {}
    VerticalSpacer(space = 14)
    EAMCustomSelectionCard(
      label = "Label",
      isRoundedCornerShape = true,
      description = "Description"
    ) {}
    VerticalSpacer(space = 14)
    EAMCustomSelectionCard(
      label = "Label",
      isRoundedCornerShape = false,
      description = "Description"
    ) {}
    VerticalSpacer(space = 14)
    
    EAMCustomSelectionCard(
      label = "Label",
      description = "Description",
      showTrailingIcon = true
    ) {}
    VerticalSpacer(space = 14)
    EAMCustomSelectionCard(
      label = "Label",
      description = null,
      showTrailingIcon = true
    ) {}
  }
}