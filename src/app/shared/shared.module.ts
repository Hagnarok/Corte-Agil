import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MaterialModule } from './material/material.module';
import { StatusBadgeComponent } from './components/status-badge.component';
import { QueueListComponent } from './components/queue-list.component';
@NgModule({
  declarations: [StatusBadgeComponent, QueueListComponent],
  imports: [CommonModule, MaterialModule],
  exports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule,
    MaterialModule,
    StatusBadgeComponent,
    QueueListComponent,
  ],
})
export class SharedModule {}
