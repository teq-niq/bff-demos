import { Component, Input, signal } from '@angular/core';
import { JsonTableComponent } from '../json-table/json-table.component';

import { computed } from '@angular/core';

@Component({
  selector: 'json-viewer',
  standalone: true,
  imports: [JsonTableComponent],
  templateUrl: './json-viewer.component.html',
  styleUrls: ['./json-viewer.component.css']
})
export class JsonViewerComponent {
  @Input() data: any;



  activeTab = signal<'table' | 'pretty' | 'source' >('table');

   select(tab: 'table' | 'pretty' | 'source' ) {
     this.activeTab.set(tab);
   }

   rawJson = computed(() => {
     try {
       return JSON.stringify(this.data);
     } catch {
       return '';
     }
   });

   prettyJson = computed(() => {
     try {
       return JSON.stringify(this.data, null, 2);
     } catch {
       return '';
     }
   });
}
